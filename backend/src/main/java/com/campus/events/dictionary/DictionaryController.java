package com.campus.events.dictionary;

import com.campus.events.common.ApiResponse;
import com.campus.events.common.ItemList;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public")
public class DictionaryController {

    private final DictionaryMapper dictionaryMapper;

    public DictionaryController(DictionaryMapper dictionaryMapper) {
        this.dictionaryMapper = dictionaryMapper;
    }

    @GetMapping("/activity-types")
    public ApiResponse<ItemList<DictionaryItem>> types() {
        return ApiResponse.ok(new ItemList<>(dictionaryMapper.listTypes()));
    }

    @GetMapping("/colleges")
    public ApiResponse<ItemList<DictionaryItem>> colleges() {
        return ApiResponse.ok(new ItemList<>(dictionaryMapper.listColleges()));
    }
}
