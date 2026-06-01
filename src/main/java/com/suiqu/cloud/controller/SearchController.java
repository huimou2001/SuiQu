package com.suiqu.cloud.controller;


import com.suiqu.cloud.entity.FileIndex;
import com.suiqu.cloud.entity.Result;
import com.suiqu.cloud.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/search")
public class SearchController {


    @Autowired
    private ElasticsearchOperations esOperations;

    @GetMapping("/files")
    public Result searchFiles(@RequestParam String keyword) {
        Long userId = SecurityUtils.getUserId();

        // 构建混合查询逻辑
        Query query = NativeQuery.builder()
                .withQuery(q -> q.bool(b -> b
                        .must(m -> m.term(t -> t.field("userId").value(userId))) // 必须是当前用户
                        .must(m -> m.bool(sb -> sb
                                .should(s -> s.multiMatch(mm -> mm
                                        .fields("name", "description")
                                        .query(keyword)
                                        .boost(2.0f) // 词条匹配的权重更高
                                ))
                                .should(s -> s.wildcard(w -> w
                                        .field("name")
                                        .value("*" + keyword + "*") // 模糊匹配文件名
                                        .caseInsensitive(true)
                                ))
                                .should(s -> s.wildcard(w -> w
                                        .field("description")
                                        .value("*" + keyword + "*") // 模糊匹配描述
                                        .caseInsensitive(true)
                                ))
                        ))
                ))
                .build();


        SearchHits<FileIndex> hits = esOperations.search(query, FileIndex.class);

        // 组装返回数据
        List<FileIndex> results = hits.stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());

        return Result.success(results);
    }
}
