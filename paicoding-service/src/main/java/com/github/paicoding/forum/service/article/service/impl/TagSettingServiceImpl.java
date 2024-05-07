package com.github.paicoding.forum.service.article.service.impl;

import com.github.paicoding.forum.api.model.vo.PageVo;
import com.github.paicoding.forum.api.model.vo.article.SearchTagReq;
import com.github.paicoding.forum.api.model.vo.article.TagReq;
import com.github.paicoding.forum.api.model.vo.article.dto.TagDTO;
import com.github.paicoding.forum.core.cache.RedisClient;
import com.github.paicoding.forum.core.util.JsonUtil;
import com.github.paicoding.forum.core.util.NumUtil;
import com.github.paicoding.forum.service.article.conveter.TagStructMapper;
import com.github.paicoding.forum.service.article.repository.dao.TagDao;
import com.github.paicoding.forum.service.article.repository.entity.TagDO;
import com.github.paicoding.forum.service.article.repository.params.SearchTagParams;
import com.github.paicoding.forum.service.article.service.TagSettingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 标签后台接口
 *
 * @author louzai
 * @date 2022-09-17
 */
@Service
public class TagSettingServiceImpl implements TagSettingService {

    private static final String CACHE_TAG_PRE = "cache_tag_pre_";

    private static final Long CACHE_TAG_EXPRIE_TIME = 100L;

    @Autowired
    private TagDao tagDao;

    /**
     * 如果tagId为null或0，插入数据，否则进行更新。并删除redis缓存
     * @param tagReq
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveTag(TagReq tagReq) {
        TagDO tagDO = TagStructMapper.INSTANCE.toDO(tagReq);

        // 先写 MySQL
        if (NumUtil.nullOrZero(tagReq.getTagId())) {
            tagDao.save(tagDO);
        } else {
            tagDO.setId(tagReq.getTagId());
            tagDao.updateById(tagDO);
        }

        // 再删除 Redis
        String redisKey = CACHE_TAG_PRE + tagDO.getId();
        RedisClient.del(redisKey);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTag(Integer tagId) {
        TagDO tagDO = tagDao.getById(tagId);
        if (tagDO != null){
            // 先写 MySQL
            tagDao.removeById(tagId);

            // 再删除 Redis
            String redisKey = CACHE_TAG_PRE + tagDO.getId();
            RedisClient.del(redisKey);
        }
    }

    @Override
    public void operateTag(Integer tagId, Integer pushStatus) {
        TagDO tagDO = tagDao.getById(tagId);
        if (tagDO != null){

            // 先写 MySQL
            tagDO.setStatus(pushStatus);
            tagDao.updateById(tagDO);

            // 再删除 Redis
            String redisKey = CACHE_TAG_PRE + tagDO.getId();
            RedisClient.del(redisKey);
        }
    }

    /**
     * 根据传入的参数获取所有tag数据，统计其数量，并封装成page对象
     * @param req
     * @return
     */
    @Override
    public PageVo<TagDTO> getTagList(SearchTagReq req) {
        // 转换
        SearchTagParams params = TagStructMapper.INSTANCE.toSearchParams(req);
        // 查询
        List<TagDTO> tagDTOS = TagStructMapper.INSTANCE.toDTOs(tagDao.listTag(params));
        Long totalCount = tagDao.countTag(params);
        return PageVo.build(tagDTOS, params.getPageSize(), params.getPageNum(), totalCount);
    }

    /**
     * 根据tagId首先从缓存获取tag信息，如果没有则从数据库获取并添加到redis缓存中
     * @param tagId
     * @return
     */
    @Override
    public TagDTO getTagById(Long tagId) {

        String redisKey = CACHE_TAG_PRE + tagId;

        // 先查询缓存，如果有就直接返回
        String tagInfoStr = RedisClient.getStr(redisKey);
        if (tagInfoStr != null && !tagInfoStr.isEmpty()) {
            return JsonUtil.toObj(tagInfoStr, TagDTO.class);
        }

        // 如果未查询到，需要先查询 DB ，再写入缓存
        TagDTO tagDTO = tagDao.selectById(tagId);
        tagInfoStr = JsonUtil.toStr(tagDTO);
        RedisClient.setStrWithExpire(redisKey, tagInfoStr, CACHE_TAG_EXPRIE_TIME);

        return tagDTO;
    }
}
