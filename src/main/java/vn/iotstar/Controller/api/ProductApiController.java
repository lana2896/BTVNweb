package vn.iotstar.Controller.api;

import java.sql.Timestamp;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import vn.iotstar.entity.Category;
import vn.iotstar.entity.Product;
import vn.iotstar.model.ProductModel;
import vn.iotstar.model.Response;
import vn.iotstar.service.ICategoryService;
import vn.iotstar.service.IProductService;
import vn.iotstar.service.IStorageService;

@RestController
@RequestMapping(path = "/api/product")
public class ProductApiController {

	@Autowired
	IProductService productService;

	@Autowired
	ICategoryService categoryService;

	@Autowired
	IStorageService storageService;

	@GetMapping
	public ResponseEntity<?> getAllProduct() {
		return new ResponseEntity<Response>(
				new Response(true, "Thành công", productService.findAll()), HttpStatus.OK);
	}

	// Tìm kiếm theo tên + phân trang: GET /api/product/page?name=&page=0&size=5
	@GetMapping(path = "/page")
	public ResponseEntity<?> getProductPage(
			@RequestParam(value = "name", required = false, defaultValue = "") String name,
			@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "size", defaultValue = "5") int size) {

		Pageable pageable = PageRequest.of(page, size);
		Page<Product> result = (name == null || name.isBlank())
				? productService.findAll(pageable)
				: productService.findByProductNameContaining(name, pageable);

		return new ResponseEntity<Response>(
				new Response(true, "Thành công", result), HttpStatus.OK);
	}

	@PostMapping(path = "/addProduct", consumes = { "multipart/form-data" })
	public ResponseEntity<?> addProduct(
			@RequestParam("productName") String productName,
			@RequestParam("imageFile") MultipartFile productImages,
			@RequestParam("unitPrice") Double productPrice,
			@RequestParam("discount") Double promotionalPrice,
			@RequestParam("description") String productDescription,
			@RequestParam("categoryId") Long categoryId,
			@RequestParam("quantity") Integer quantity,
			@RequestParam("status") Short status) {

		Optional<Product> optProduct = productService.findByProductName(productName);
		if (optProduct.isPresent()) {
			return new ResponseEntity<Response>(
					new Response(false, "Sản phẩm này đã tồn tại trong hệ thống", optProduct.get()),
					HttpStatus.BAD_REQUEST);
		} else {
			Product product = new Product();
			Timestamp timestamp = new Timestamp(new Date(System.currentTimeMillis()).getTime());
			try {
				// gán dữ liệu từ tham số vào ProductModel
				// (đây là bước PDF gốc bị thiếu, khiến proModel luôn rỗng)
				ProductModel proModel = new ProductModel();
				proModel.setProductName(productName);
				proModel.setImageFile(productImages);
				proModel.setUnitPrice(productPrice);
				proModel.setDiscount(promotionalPrice);
				proModel.setDescription(productDescription);
				proModel.setCategoryId(categoryId);
				proModel.setQuantity(quantity);
				proModel.setStatus(status);

				// copy từ Model sang Entity
				BeanUtils.copyProperties(proModel, product);
				product.setProductId(null);

				// xử lý category liên quan product
				Category cateEntity = new Category();
				cateEntity.setCategoryId(proModel.getCategoryId());
				product.setCategory(cateEntity);

				// kiểm tra tồn tại file, lưu file
				if (!proModel.getImageFile().isEmpty()) {
					UUID uuid = UUID.randomUUID();
					String uuString = uuid.toString();
					//lưu file vào trường Images
					product.setImages(storageService.getSorageFilename(proModel.getImageFile(), uuString));
					storageService.store(proModel.getImageFile(), product.getImages());
				}

				product.setCreateDate(timestamp);
				productService.save(product);
				optProduct = productService.findByCreateDate(timestamp);
			} catch (Exception e) {
				e.printStackTrace();
				return new ResponseEntity<Response>(
						new Response(false, "Có lỗi khi thêm sản phẩm: " + e.getMessage(), null),
						HttpStatus.INTERNAL_SERVER_ERROR);
			}
			// return ResponseEntity.ok().body(product); 
			return new ResponseEntity<Response>(
					new Response(true, "Thành công", optProduct.get()), HttpStatus.OK);
		}
	}

	@PutMapping(path = "/updateProduct", consumes = { "multipart/form-data" })
	public ResponseEntity<?> updateProduct(@ModelAttribute ProductModel proModel) {

		Optional<Product> optProduct = productService.findById(proModel.getProductId());
		if (optProduct.isEmpty()) {
			return new ResponseEntity<Response>(
					new Response(false, "Không tìm thấy sản phẩm", null), HttpStatus.BAD_REQUEST);
		}

		Product product = optProduct.get();
		if (proModel.getProductName() != null) {
			product.setProductName(proModel.getProductName());
		}
		if (proModel.getUnitPrice() != null) {
			product.setUnitPrice(proModel.getUnitPrice());
		}
		if (proModel.getDiscount() != null) {
			product.setDiscount(proModel.getDiscount());
		}
		if (proModel.getDescription() != null) {
			product.setDescription(proModel.getDescription());
		}
		if (proModel.getQuantity() != null) {
			product.setQuantity(proModel.getQuantity());
		}
		if (proModel.getStatus() != null) {
			product.setStatus(proModel.getStatus());
		}

		if (proModel.getCategoryId() != null) {
			Category cateEntity = new Category();
			cateEntity.setCategoryId(proModel.getCategoryId());
			product.setCategory(cateEntity);
		}

		if (proModel.getImageFile() != null && !proModel.getImageFile().isEmpty()) {
			UUID uuid = UUID.randomUUID();
			String uuString = uuid.toString();
			product.setImages(storageService.getSorageFilename(proModel.getImageFile(), uuString));
			storageService.store(proModel.getImageFile(), product.getImages());
		}

		productService.save(product);
		return new ResponseEntity<Response>(
				new Response(true, "Cập nhật Thành công", product), HttpStatus.OK);
	}

	@DeleteMapping(path = "/deleteProduct")
	public ResponseEntity<?> deleteProduct(@RequestParam("productId") Long productId) {
		Optional<Product> optProduct = productService.findById(productId);
		if (optProduct.isEmpty()) {
			return new ResponseEntity<Response>(
					new Response(false, "Không tìm thấy sản phẩm", null), HttpStatus.BAD_REQUEST);
		}
		productService.delete(optProduct.get());
		return new ResponseEntity<Response>(
				new Response(true, "Xóa Thành công", optProduct.get()), HttpStatus.OK);
	}


	@GetMapping(path = "/images/{filename:.+}", produces = MediaType.ALL_VALUE)
	public ResponseEntity<Resource> serveFile(@PathVariable String filename) {
		Resource file = storageService.loadAsResource(filename);
		MediaType contentType = MediaTypeFactory.getMediaType(file)
				.orElse(MediaType.APPLICATION_OCTET_STREAM);

		return ResponseEntity.ok()
				.contentType(contentType)
				.header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getFilename() + "\"")
				.body(file);
	}
}