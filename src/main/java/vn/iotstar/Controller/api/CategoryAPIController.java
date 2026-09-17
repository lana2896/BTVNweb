package vn.iotstar.Controller.api;

import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import vn.iotstar.entity.Category;
import vn.iotstar.model.Response;
import vn.iotstar.service.ICategoryService;
import vn.iotstar.service.IStorageService;

@RestController
@RequestMapping(path = "/api/category")
public class CategoryAPIController {

	@Autowired
	private ICategoryService categoryService;

	@Autowired
	IStorageService storageService;

	@GetMapping
	public ResponseEntity<?> getAllCategory() {
		// return ResponseEntity.ok().body(categoryService.findAll());
		return new ResponseEntity<Response>(
				new Response(true, "Thành công", categoryService.findAll()), HttpStatus.OK);
	}

	@PostMapping(path = "/getCategory")
	public ResponseEntity<?> getCategory(@Validated @RequestParam("id") Long id) {
		Optional<Category> category = categoryService.findById(id);
		if (category.isPresent()) {
			return new ResponseEntity<Response>(
					new Response(true, "Thành công", category.get()), HttpStatus.OK);
		} else {
			return new ResponseEntity<Response>(
					new Response(false, "Thất bại", null), HttpStatus.NOT_FOUND);
		}
	}

	@PostMapping(path = "/addCategory", consumes = { "multipart/form-data" })
	public ResponseEntity<?> addCategory(@Validated @RequestParam("categoryName") String categoryName,
			@RequestParam(name = "icon", required = false) MultipartFile icon) {

		Optional<Category> optCategory = categoryService.findByCategoryName(categoryName);
		if (optCategory.isPresent()) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body("Category đã tồn tại trong hệ thống");
		} else {
			Category category = new Category();

			// kiểm tra tồn tại file, lưu file
			if (icon != null && !icon.isEmpty()) {
				UUID uuid = UUID.randomUUID();
				String uuString = uuid.toString();
				// lưu tên file vào trường icon
				category.setIcon(storageService.getSorageFilename(icon, uuString));
				storageService.store(icon, category.getIcon());
			}

			category.setCategoryName(categoryName);
			categoryService.save(category);

			return new ResponseEntity<Response>(
					new Response(true, "Thêm Thành công", category), HttpStatus.OK);
		}
	}

	@PutMapping(path = "/updateCategory", consumes = { "multipart/form-data" })
	public ResponseEntity<?> updateCategory(@Validated @RequestParam("categoryId") Long categoryId,
			@Validated @RequestParam("categoryName") String categoryName,
			@RequestParam(name = "icon", required = false) MultipartFile icon) {

		Optional<Category> optCategory = categoryService.findById(categoryId);
		if (optCategory.isEmpty()) {
			return new ResponseEntity<Response>(
					new Response(false, "Không tìm thấy Category", null), HttpStatus.BAD_REQUEST);
		} else {
			// kiểm tra tồn tại file, lưu file
			if (icon != null && !icon.isEmpty()) {
				UUID uuid = UUID.randomUUID();
				String uuString = uuid.toString();
				optCategory.get().setIcon(storageService.getSorageFilename(icon, uuString));
				storageService.store(icon, optCategory.get().getIcon());
			}

			optCategory.get().setCategoryName(categoryName);
			categoryService.save(optCategory.get());

			return new ResponseEntity<Response>(
					new Response(true, "Cập nhật Thành công", optCategory.get()), HttpStatus.OK);
		}
	}

	@DeleteMapping(path = "/deleteCategory")
	public ResponseEntity<?> deleteCategory(@Validated @RequestParam("categoryId") Long categoryId) {
		Optional<Category> optCategory = categoryService.findById(categoryId);
		if (optCategory.isEmpty()) {
			return new ResponseEntity<Response>(
					new Response(false, "Không tìm thấy Category", null), HttpStatus.BAD_REQUEST);
		} else {
			categoryService.delete(optCategory.get());
			return new ResponseEntity<Response>(
					new Response(true, "Xóa Thành công", optCategory.get()), HttpStatus.OK);
		}
	}

	// Xem ảnh icon đã upload: GET /api/category/images/{filename}
	@GetMapping("/images/{filename:.+}")
	public ResponseEntity<Resource> serveFile(@PathVariable String filename) {
		Resource file = storageService.loadAsResource(filename);
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getFilename() + "\"")
				.body(file);
	}
}
