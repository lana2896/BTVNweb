package vn.iotstar.model;

import org.springframework.web.multipart.MultipartFile;

import lombok.Data;

@Data
public class ProductModel {
	private Long productId;
	private String productName;
	private MultipartFile imageFile;
	private Double unitPrice;
	private Double discount;
	private String description;
	private Long categoryId;
	private Integer quantity;
	private Short status;
}
