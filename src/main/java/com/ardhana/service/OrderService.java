package com.ardhana.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ardhana.dto.CheckoutInitResponse;
import com.ardhana.dto.CheckoutRequest;
import com.ardhana.dto.OrderResponse;
import com.ardhana.dto.PaymentVerificationRequest;
import com.ardhana.model.Enrollment;
import com.ardhana.model.Order;
import com.ardhana.model.OrderItem;
import com.ardhana.model.OrderStatus;
import com.ardhana.repository.CourseRepository;
import com.ardhana.repository.EnrollmentRepository;
import com.ardhana.repository.OrderRepository;
import com.razorpay.RazorpayException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final RazorpayService razorpayService;

    /**
     * Testing-only escape hatch: when true, checkout never talks to
     * Razorpay at all — no real order is created and no real signature is
     * checked. Lets you exercise the full checkout -> enrollment flow
     * locally (or on a test deployment) without live/test Razorpay keys.
     * Set via PAYMENTS_MOCK_ENABLED=true. Defaults to false — must be
     * explicitly turned on, never on by accident in production.
     */
    @Value("${app.payments.mock-enabled:false}")
    private boolean mockPaymentsEnabled;

    public OrderService(OrderRepository orderRepository,
            EnrollmentRepository enrollmentRepository,
            CourseRepository courseRepository,
            RazorpayService razorpayService) {
        this.orderRepository = orderRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.courseRepository = courseRepository;
        this.razorpayService = razorpayService;
    }

    /**
     * Step 1 of checkout: persist a PENDING order for the cart and open a
     * matching order on Razorpay. The order is NOT confirmed and no
     * enrollments are created until the payment is verified (see
     * {@link #confirmPayment}) or a webhook confirms it.
     */
    @Transactional
    public CheckoutInitResponse initiateCheckout(CheckoutRequest request) {
        log.info("Checkout initiated: userId={}, items={}, totalAmount={}",
                request.getUserId(),
                request.getItems() == null ? 0 : request.getItems().size(),
                request.getTotalAmount());

        // Razorpay orders are single-currency — every item in the cart must
        // share one currency by the time it reaches checkout (enforced when
        // items are added to the cart), but double-check here too.
        String orderCurrency = null;
        for (CheckoutRequest.CheckoutItemRequest i : request.getItems()) {
            String itemCurrency = (i.getCurrency() == null || i.getCurrency().isBlank())
                    ? "INR" : i.getCurrency().toUpperCase();
            if (orderCurrency == null) {
                orderCurrency = itemCurrency;
            } else if (!orderCurrency.equals(itemCurrency)) {
                log.warn("Checkout rejected for userId={}: mixed currencies {} and {}",
                        request.getUserId(), orderCurrency, itemCurrency);
                throw new IllegalArgumentException("Cart items have mixed currencies (" + orderCurrency + " and " + itemCurrency + ") — cannot check out in a single order.");
            }
        }
        if (orderCurrency == null) {
            orderCurrency = razorpayService.getCurrency();
        }

        List<OrderItem> orderItems = new ArrayList<>();
        for (CheckoutRequest.CheckoutItemRequest i : request.getItems()) {
            OrderItem item = new OrderItem();
            item.setPillarId(i.getPillarId());
            item.setPillarName(i.getPillarName());
            item.setPrice(i.getPrice());
            item.setCurrency(orderCurrency);
            item.setQuantity(i.getQuantity());
            orderItems.add(item);
        }

        Order order = new Order();
        order.setUserId(request.getUserId());
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(request.getTotalAmount());
        order.setCurrency(orderCurrency);
        order.setItems(orderItems);
        orderItems.forEach(item -> item.setOrder(order));

        Order saved = orderRepository.save(order);
        log.debug("Persisted PENDING order id={} for userId={}, currency={}, totalAmount={}",
                saved.getId(), request.getUserId(), orderCurrency, request.getTotalAmount());

        if (mockPaymentsEnabled) {
            String mockRazorpayOrderId = "mock_order_" + saved.getId();
            log.warn("PAYMENTS_MOCK_ENABLED=true — skipping real Razorpay order creation for localOrderId={} "
                    + "(mockRazorpayOrderId={}). Do NOT enable this in production.",
                    saved.getId(), mockRazorpayOrderId);
            saved.setRazorpayOrderId(mockRazorpayOrderId);
            orderRepository.save(saved);
            return new CheckoutInitResponse(
                    saved.getId(),
                    mockRazorpayOrderId,
                    request.getTotalAmount(),
                    orderCurrency,
                    razorpayService.getKeyId(),
                    true);
        }

        String razorpayOrderId;
        try {
            log.info("Creating Razorpay order for local order id={}: amount={}, currency={}",
                    saved.getId(), request.getTotalAmount(), orderCurrency);
            razorpayOrderId = razorpayService.createOrder(request.getTotalAmount(), orderCurrency, saved.getId().toString());
            log.info("Razorpay order created: localOrderId={}, razorpayOrderId={}", saved.getId(), razorpayOrderId);
        } catch (RazorpayException e) {
            log.error("Razorpay order creation FAILED for localOrderId={} (amount={}, currency={}): {}",
                    saved.getId(), request.getTotalAmount(), orderCurrency, e.getMessage(), e);
            saved.setStatus(OrderStatus.FAILED);
            orderRepository.save(saved);
            throw new IllegalStateException("Unable to create Razorpay order: " + e.getMessage(), e);
        }

        saved.setRazorpayOrderId(razorpayOrderId);
        orderRepository.save(saved);

        return new CheckoutInitResponse(
                saved.getId(),
                razorpayOrderId,
                request.getTotalAmount(),
                orderCurrency,
                razorpayService.getKeyId(),
                false);
    }

    /**
     * Step 2 of checkout: called by the frontend once Razorpay's Checkout
     * handler callback fires with a payment id + signature. Verifies the
     * signature server-side before trusting the payment, then confirms the
     * order and creates enrollments.
     */
    @Transactional
    public OrderResponse confirmPayment(PaymentVerificationRequest request) {
        log.info("Verifying payment: orderId={}, razorpayOrderId={}, razorpayPaymentId={}",
                request.getOrderId(), request.getRazorpayOrderId(), request.getRazorpayPaymentId());

        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> {
                    log.warn("Payment verification failed: order not found id={}", request.getOrderId());
                    return new IllegalArgumentException("Order not found: " + request.getOrderId());
                });

        // Already confirmed (e.g. webhook beat the client callback) — idempotent no-op.
        if (order.getStatus() == OrderStatus.CONFIRMED) {
            log.info("Order {} already CONFIRMED (likely via webhook) — treating as no-op", order.getId());
            return toResponse(order);
        }

        if (order.getRazorpayOrderId() == null || !order.getRazorpayOrderId().equals(request.getRazorpayOrderId())) {
            log.warn("Payment verification failed for order {}: razorpayOrderId mismatch (expected {}, got {})",
                    order.getId(), order.getRazorpayOrderId(), request.getRazorpayOrderId());
            throw new IllegalArgumentException("Razorpay order id does not match this order");
        }

        // Mock mode: the razorpayOrderId match check above already confirmed
        // this verify-payment call belongs to an order this server itself
        // created in mock mode (real razorpayOrderIds never start with
        // "mock_order_") — skip real signature verification since there's
        // no real Razorpay payment to check a signature against.
        boolean valid = mockPaymentsEnabled
                ? true
                : razorpayService.verifyPaymentSignature(
                        request.getRazorpayOrderId(),
                        request.getRazorpayPaymentId(),
                        request.getRazorpaySignature());
        if (mockPaymentsEnabled) {
            log.warn("PAYMENTS_MOCK_ENABLED=true — skipping real signature verification for order {}", order.getId());
        }

        if (!valid) {
            log.warn("Payment signature verification FAILED for order {} (razorpayOrderId={}, razorpayPaymentId={})",
                    order.getId(), request.getRazorpayOrderId(), request.getRazorpayPaymentId());
            order.setStatus(OrderStatus.FAILED);
            orderRepository.save(order);
            throw new IllegalArgumentException("Payment signature verification failed");
        }

        order.setStatus(OrderStatus.CONFIRMED);
        order.setRazorpayPaymentId(request.getRazorpayPaymentId());
        order.setRazorpaySignature(request.getRazorpaySignature());
        Order saved = orderRepository.save(order);
        log.info("Order {} CONFIRMED via client verify-payment call", saved.getId());

        createEnrollmentsForOrder(saved);

        return toResponse(saved);
    }

    /**
     * Called by the Razorpay webhook handler as a durable fallback in case the
     * client never calls back (dropped connection, closed tab, etc). Safe to
     * call more than once for the same order.
     */
    @Transactional
    public void confirmPaymentFromWebhook(String razorpayOrderId, String razorpayPaymentId) {
        log.info("Webhook payment.captured received: razorpayOrderId={}, razorpayPaymentId={}",
                razorpayOrderId, razorpayPaymentId);

        Optional<Order> maybeOrder = orderRepository.findByRazorpayOrderId(razorpayOrderId);
        if (maybeOrder.isEmpty()) {
            log.warn("Webhook payment.captured for unknown razorpayOrderId={}", razorpayOrderId);
            return;
        }

        Order order = maybeOrder.get();
        if (order.getStatus() == OrderStatus.CONFIRMED) {
            log.debug("Webhook: order {} already CONFIRMED — ignoring duplicate event", order.getId());
            return;
        }

        order.setStatus(OrderStatus.CONFIRMED);
        order.setRazorpayPaymentId(razorpayPaymentId);
        Order saved = orderRepository.save(order);
        log.info("Order {} CONFIRMED via webhook", saved.getId());

        createEnrollmentsForOrder(saved);
    }

    public List<OrderResponse> getOrdersByUser(UUID userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId)
            .stream().map(this::toResponse).collect(Collectors.toList());
    }

    private void createEnrollmentsForOrder(Order order) {
        for (OrderItem i : order.getItems()) {
            String courseId = null;

            if (i.getPillarId() != null) {
                courseId = i.getPillarId().toString();
            } else if (i.getPillarName() != null) {
                var course = courseRepository.findByTitle(i.getPillarName());
                if (course.isPresent()) {
                    courseId = course.get().getId();
                }
            }

            if (courseId != null) {
                boolean alreadyEnrolled = enrollmentRepository
                    .existsByCourseIdAndStudentId(courseId, order.getUserId().toString());
                if (!alreadyEnrolled) {
                    Enrollment enrollment = new Enrollment();
                    enrollment.setCourseId(courseId);
                    enrollment.setStudentId(order.getUserId().toString());
                    enrollmentRepository.save(enrollment);
                }
            }
        }
    }

    private OrderResponse toResponse(Order order) {
        List<OrderResponse.OrderItemResponse> itemResponses = order.getItems().stream()
            .map(i -> {
                OrderResponse.OrderItemResponse res = new OrderResponse.OrderItemResponse();
                res.setPillarId(i.getPillarId());
                res.setPillarName(i.getPillarName());
                res.setPrice(i.getPrice());
                res.setCurrency(i.getCurrency());
                res.setQuantity(i.getQuantity());
                res.setSubtotal(i.getPrice().multiply(BigDecimal.valueOf(i.getQuantity())));
                return res;
            })
            .collect(Collectors.toList());

        OrderResponse response = new OrderResponse();
        response.setOrderId(order.getId());
        response.setStatus(order.getStatus());
        response.setTotalAmount(order.getTotalAmount());
        response.setCurrency(order.getCurrency());
        response.setItems(itemResponses);
        response.setCreatedAt(order.getCreatedAt());
        return response;
    }
}
