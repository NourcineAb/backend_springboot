package com.enterprise.poodle.mapper;

import com.enterprise.poodle.dto.response.CertificateResponse;
import com.enterprise.poodle.entity.Certificate;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CertificateMapper {

    @Mapping(target = "employeeName", expression = "java(certificate.getEmployee().getFirstName() + \" \" + certificate.getEmployee().getLastName())")
    @Mapping(source = "course.title", target = "courseTitle")
    CertificateResponse toResponse(Certificate certificate);
}
