package com.involveininnovation.chat.entity;

import lombok.Data;
import lombok.ToString;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.util.Date;

@Data
@MappedSuperclass
@ToString
@EntityListeners(AuditingEntityListener.class)
public class CreateAuditable {
    @CreatedDate
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at", nullable = false, updatable = false)
    private Date createdAt;

    @CreatedBy
    @JoinColumn(name = "created_by", nullable = false, updatable = false)
    @ManyToOne
    private User createdBy;
}
