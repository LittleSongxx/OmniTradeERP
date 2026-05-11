package com.crossborder.erp.product.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.crossborder.erp.common.result.PageResult;
import com.crossborder.erp.common.result.Result;
import com.crossborder.erp.product.entity.Product;
import com.crossborder.erp.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/product")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public Result<Product> saveProduct(@RequestBody Product product) {
        Product saved = productService.saveProduct(product);
        return Result.success(saved);
    }

    @GetMapping("/{id}")
    public Result<Product> getProduct(@PathVariable Long id) {
        Product product = productService.getProductById(id);
        return Result.success(product);
    }

    @GetMapping("/sku/{sku}")
    public Result<Product> getProductBySku(@PathVariable String sku) {
        Product product = productService.getProductBySku(sku);
        return Result.success(product);
    }

    @GetMapping("/list")
    public Result<PageResult<Product>> listProducts(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String productName,
            @RequestParam(required = false) Long categoryId) {

        Page<Product> page = new Page<>(current, size);
        IPage<Product> result = productService.pageProducts(page, productName, categoryId);
        // MyBatis-Plus count workaround: ensure total is accurate
        PageResult<Product> pageResult = PageResult.of(result);
        if (pageResult.getTotal() == 0 && !result.getRecords().isEmpty()) {
            pageResult.setTotal((long) result.getRecords().size());
        }
        return Result.success(pageResult);
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return Result.success();
    }
}
