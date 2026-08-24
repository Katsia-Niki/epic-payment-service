package by.nikiforova.payment.mapper;

import by.nikiforova.payment.dto.request.PaymentRequestDto;
import by.nikiforova.payment.dto.response.PaymentResponseDto;
import by.nikiforova.payment.entity.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaymentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "timestamp", ignore = true)
    Payment toEntity(PaymentRequestDto paymentRequestDto);

    PaymentResponseDto toResponseDto(Payment payment);

}
