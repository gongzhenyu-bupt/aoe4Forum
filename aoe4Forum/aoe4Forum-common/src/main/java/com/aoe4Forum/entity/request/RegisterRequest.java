package com.aoe4Forum.entity.request;

import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import static com.aoe4Forum.entity.constans.Constants.PASSWORD_REGEX;

@Data
public class RegisterRequest {
        @NotEmpty @Size(max = 128) String name;
        @NotEmpty @Pattern(regexp = PASSWORD_REGEX) String password;
        @NotEmpty @Length(min=11,max=11) String phoneNo;
        String checkCode;
        String checkCodeKey;
}
