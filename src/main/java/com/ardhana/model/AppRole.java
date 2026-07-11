package com.ardhana.model;

public enum AppRole {
    admin,
    teacher,
    student,
    // A school's own login (created by an admin via SchoolAuthService), used
    // only to view/pay the school's platform fee — not a person.
    school
}
