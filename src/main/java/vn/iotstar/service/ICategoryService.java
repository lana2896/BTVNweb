package vn.iotstar.service;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import vn.iotstar.entity.Category;

public interface ICategoryService {

	void delete(Category entity);

	void deleteById(Long id);

	long count();

	<S extends Category> Optional<S> findOne(Example<S> example);

	Optional<Category> findById(Long id);

	List<Category> findAllById(Iterable<Long> ids);

	List<Category> findAll(Sort sort);

	Page<Category> findAll(Pageable pageable);

	List<Category> findAll();

	Optional<Category> findByCategoryName(String name);

	<S extends Category> S save(S entity);

	Page<Category> findByCategoryNameContaining(String name, Pageable pageable);

	List<Category> findByCategoryNameContaining(String name);
}
