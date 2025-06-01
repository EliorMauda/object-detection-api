package com.objectdetection.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoundingBox {
    private Float xMin;
    private Float yMin;
    private Float xMax;
    private Float yMax;
}