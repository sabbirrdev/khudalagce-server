package com.company.efood.base;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class BaseDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    @JsonProperty("active")
    @JsonAlias({"isActive", "is_active", "status"})
    private Boolean active = true;

    private LocalDateTime entryDate;

    @JsonProperty("isActive")
    public Boolean getIsActive() {
        return active != null ? active : true;
    }

    @JsonProperty("isActive")
    public void setIsActive(Boolean isActive) {
        if (isActive != null) {
            this.active = isActive;
        }
    }
}
