package com.alin.lin.controller;

import com.alin.lin.dto.PosChangeRequest;
import com.alin.lin.dto.ResponseBodyDto;
import com.alin.lin.entity.PosChange;
import com.alin.lin.service.PosChangeService;
import com.alin.lin.util.ResponseUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/pos-changes")
public class PosChangeController {
    private final PosChangeService posChangeService;

    public PosChangeController(PosChangeService posChangeService) {
        this.posChangeService = posChangeService;
    }

    @GetMapping
    public ResponseEntity<ResponseBodyDto<List<PosChange>>> findAll() {
        return ResponseUtil.ok(posChangeService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResponseBodyDto<PosChange>> findById(@PathVariable Long id) {
        return ResponseUtil.ok(posChangeService.findById(id));
    }

    @PostMapping
    public ResponseEntity<ResponseBodyDto<PosChange>> create(@RequestBody PosChangeRequest request) {
        return ResponseUtil.created(posChangeService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ResponseBodyDto<PosChange>> update(@PathVariable Long id, @RequestBody PosChangeRequest request) {
        return ResponseUtil.ok(posChangeService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseBodyDto<Void>> delete(@PathVariable Long id) {
        posChangeService.delete(id);
        return ResponseUtil.noContent("刪除成功");
    }
}
