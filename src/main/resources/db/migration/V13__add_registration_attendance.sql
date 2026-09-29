ALTER TABLE workshop.registration
    ADD COLUMN attendance_status VARCHAR(24),
    ADD COLUMN attendance_marked_at TIMESTAMPTZ,
    ADD COLUMN attendance_marked_by UUID REFERENCES workshop.app_user(id),
    ADD CONSTRAINT registration_attendance_status_check
        CHECK (attendance_status IS NULL OR attendance_status IN (
            'ATTENDED', 'NOT_ATTENDED', 'JUSTIFIED_ABSENCE', 'ABSENT'
        )),
    ADD CONSTRAINT registration_attendance_audit_check
        CHECK (
            (attendance_status IS NULL AND attendance_marked_at IS NULL AND attendance_marked_by IS NULL)
            OR
            (attendance_status IS NOT NULL AND attendance_marked_at IS NOT NULL AND attendance_marked_by IS NOT NULL)
        );

CREATE INDEX registration_workshop_attendance
    ON workshop.registration (workshop_id, attendance_status, registered_at, id);
