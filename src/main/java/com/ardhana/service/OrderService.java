package com.ardhana.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ardhana.dto.CheckoutRequest;
import com.ardhana.dto.OrderResponse;
import com.ardhana.model.Enrollment;
import com.ardhana.model.Order;
import com.ardhana.model.OrderItem;
import com.ardhana.model.OrderStatus;
import com.ardhana.repository.CourseRepository;
import com.ardhana.repository.EnrollmentRepository;
import com.ardhana.repository.OrderRepository;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    
    public OrderService(OrderRepository orderRepository,
            EnrollmentRepository enrollmentRepository,
            CourseRepository courseRepository) {
		this.orderRepository = orderRepository;
		this.enrollmentRepository = enrollmentRepository;
		this.courseRepository = courseRepository;
	}
    
    @Transactional
    public OrderResponse placeOrder(CheckoutRequest request) {
        List<OrderItem> orderItems = new ArrayList<>();
        for (CheckoutRequest.CheckoutItemRequest i : request.getItems()) {
            OrderItem item = new OrderItem();
            item.setPillarId(i.getPillarId());
            item.setPillarName(i.getPillarName());
            item.setPrice(i.getPrice());
            item.setQuantity(i.getQuantity());
            orderItems.add(item);
        }

        Order order = new Order();
        order.setUserId(request.getUserId());
        order.setStatus(OrderStatus.CONFIRMED);
        order.setTotalAmount(request.getTotalAmount());
        order.setItems(orderItems);
        orderItems.forEach(item -> item.setOrder(order));

        Order saved = orderRepository.save(order);

        // Create enrollments using pillarName to find course
        for (CheckoutRequest.CheckoutItemRequest i : request.getItems()) {
            String courseId = null;

            // Try pillarId first
            if (i.getPillarId() != null) {
                courseId = i.getPillarId().toString();
            } else if (i.getPillarName() != null) {
                // Fall back to finding course by title
                courseRepository.findByTitle(i.getPillarName())
                    .ifPresent(c -> {});
                var course = courseRepository.findByTitle(i.getPillarName());
                if (course.isPresent()) {
                    courseId = course.get().getId();
                }
            }

            if (courseId != null) {
                boolean alreadyEnrolled = enrollmentRepository
                    .existsByCourseIdAndStudentId(courseId, request.getUserId().toString());
                if (!alreadyEnrolled) {
                    Enrollment enrollment = new Enrollment();
                    enrollment.setCourseId(courseId);
                    enrollment.setStudentId(request.getUserId().toString());
                    enrollmentRepository.save(enrollment);
                }
            }
        }

        return toResponse(saved);
    }
    
    public List<OrderResponse> getOrdersByUser(UUID userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId)
            .stream().map(this::toResponse).collect(Collectors.toList());
    }

    private OrderResponse toResponse(Order order) {
        List<OrderResponse.OrderItemResponse> itemResponses = order.getItems().stream()
            .map(i -> {
                OrderResponse.OrderItemResponse res = new OrderResponse.OrderItemResponse();
                res.setPillarId(i.getPillarId());
                res.setPillarName(i.getPillarName());
                res.setPrice(i.getPrice());
                res.setQuantity(i.getQuantity());
                res.setSubtotal(i.getPrice().multiply(BigDecimal.valueOf(i.getQuantity())));
                return res;
            })
            .collect(Collectors.toList());

        OrderResponse response = new OrderResponse();
        response.setOrderId(order.getId());
        response.setStatus(order.getStatus());
        response.setTotalAmount(order.getTotalAmount());
        response.setItems(itemResponses);
        response.setCreatedAt(order.getCreatedAt());
        return response;
    }
}