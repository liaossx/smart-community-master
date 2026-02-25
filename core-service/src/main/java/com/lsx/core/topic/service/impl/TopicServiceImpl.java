package com.lsx.core.topic.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lsx.core.topic.dto.TopicAuditDTO;
import com.lsx.core.topic.dto.TopicCommentDTO;
import com.lsx.core.topic.dto.TopicCreateDTO;
import com.lsx.core.topic.entity.Topic;
import com.lsx.core.topic.entity.TopicComment;
import com.lsx.core.topic.entity.TopicLike;
import com.lsx.core.topic.enums.TopicStatusEnum;
import com.lsx.core.topic.mapper.TopicCommentMapper;
import com.lsx.core.topic.mapper.TopicLikeMapper;
import com.lsx.core.topic.mapper.TopicMapper;
import com.lsx.core.topic.service.TopicService;
import com.lsx.core.topic.vo.TopicCommentVO;
import com.lsx.core.topic.vo.TopicVO;
import com.lsx.core.user.entity.User;
import com.lsx.core.user.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TopicServiceImpl extends ServiceImpl<TopicMapper, Topic> implements TopicService {

    private static final int PREVIEW_COMMENT_LIMIT = 3;

    @Autowired
    private TopicMapper topicMapper;
    @Autowired
    private TopicCommentMapper topicCommentMapper;
    @Autowired
    private TopicLikeMapper topicLikeMapper;
    @Autowired
    private UserMapper userMapper;

    @Override
    @Transactional
    public Long createTopic(TopicCreateDTO dto) {
        if (dto == null || dto.getUserId() == null) {
            throw new RuntimeException("发帖人不能为空");
        }
        if (!StringUtils.hasText(dto.getTitle()) || !StringUtils.hasText(dto.getContent())) {
            throw new RuntimeException("标题或内容不能为空");
        }
        Topic topic = new Topic();
        topic.setUserId(dto.getUserId());
        topic.setTitle(dto.getTitle());
        topic.setContent(dto.getContent());
        topic.setImages(convertImagesToString(dto.getImageUrls()));
        topic.setStatus(TopicStatusEnum.PENDING.name());
        topic.setLikeCount(0);
        topic.setCommentCount(0);
        topic.setDeleted(false);
        topic.setCreateTime(LocalDateTime.now());
        topic.setUpdateTime(LocalDateTime.now());
        topicMapper.insert(topic);
        log.info("用户[{}]创建话题[{}]", dto.getUserId(), topic.getId());
        return topic.getId();
    }

    @Override
    public Page<TopicVO> pageTopics(Integer pageNum, Integer pageSize, String status) {
        Page<Topic> page = new Page<>(pageNum == null ? 1 : pageNum, pageSize == null ? 10 : pageSize);
        LambdaQueryWrapper<Topic> query = Wrappers.<Topic>lambdaQuery()
                .eq(Topic::getDeleted, false)
                .orderByDesc(Topic::getCreateTime);

        TopicStatusEnum statusEnum = TopicStatusEnum.fromString(status);
        if (statusEnum != null) {
            query.eq(Topic::getStatus, statusEnum.name());
        } else {
            query.eq(Topic::getStatus, TopicStatusEnum.APPROVED.name());
        }

        Page<Topic> topicPage = topicMapper.selectPage(page, query);
        List<Topic> records = topicPage.getRecords();
        List<Long> topicIds = records.stream().map(Topic::getId).collect(Collectors.toList());

        Map<Long, List<TopicCommentVO>> commentPreviewMap = fetchLatestComments(topicIds);

        List<TopicVO> voList = records.stream().map(topic -> {
            TopicVO vo = new TopicVO();
            BeanUtils.copyProperties(topic, vo);
            vo.setImageUrls(convertImagesToList(topic.getImages()));
            vo.setLatestComments(commentPreviewMap.getOrDefault(topic.getId(), Collections.emptyList()));
            return vo;
        }).collect(Collectors.toList());

        Page<TopicVO> voPage = new Page<>(topicPage.getCurrent(), topicPage.getSize(), topicPage.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    @Transactional
    public Long addComment(Long topicId, TopicCommentDTO dto) {
        Topic topic = topicMapper.selectById(topicId);
        if (topic == null || Boolean.TRUE.equals(topic.getDeleted())) {
            throw new RuntimeException("话题不存在");
        }
        if (!TopicStatusEnum.APPROVED.name().equals(topic.getStatus())) {
            throw new RuntimeException("仅已审核通过的话题支持评论");
        }
        if (dto == null || dto.getUserId() == null || !StringUtils.hasText(dto.getContent())) {
            throw new RuntimeException("评论内容非法");
        }

        TopicComment comment = new TopicComment();
        comment.setTopicId(topicId);
        comment.setUserId(dto.getUserId());
        comment.setContent(dto.getContent());
        comment.setCreateTime(LocalDateTime.now());

        // ================= 核心：楼中楼处理 =================

        // 一级评论
        if (dto.getParentId() == null) {
            comment.setParentId(null);
            topicCommentMapper.insert(comment);

            // 一级评论：root_id = 自己
            comment.setRootId(comment.getId());
            topicCommentMapper.updateById(comment);
        }
        // 回复评论（楼中楼）
        else {
            TopicComment parent = topicCommentMapper.selectById(dto.getParentId());
            if (parent == null) {
                throw new RuntimeException("父评论不存在");
            }

            comment.setParentId(parent.getId());
            comment.setRootId(parent.getRootId());
            topicCommentMapper.insert(comment);
        }

        // 评论数 +1
        topicMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Topic>()
                        .eq(Topic::getId, topicId)
                        .set(Topic::getUpdateTime, LocalDateTime.now())
                        .setSql("comment_count = comment_count + 1")
        );

        return comment.getId();
    }

    @Override
    @Transactional
    public Boolean likeTopic(Long topicId, Long userId) {
        Topic topic = topicMapper.selectById(topicId);
        if (topic == null || Boolean.TRUE.equals(topic.getDeleted())) {
            throw new RuntimeException("话题不存在");
        }
        if (userId == null) {
            throw new RuntimeException("用户ID不能为空");
        }
        LambdaQueryWrapper<TopicLike> wrapper = Wrappers.<TopicLike>lambdaQuery()
                .eq(TopicLike::getTopicId, topicId)
                .eq(TopicLike::getUserId, userId);
        TopicLike like = topicLikeMapper.selectOne(wrapper);
        if (like != null) {
            return true; // 幂等控制
        }
        TopicLike record = new TopicLike();
        record.setTopicId(topicId);
        record.setUserId(userId);
        record.setCreateTime(LocalDateTime.now());
        topicLikeMapper.insert(record);

        LambdaUpdateWrapper<Topic> updateWrapper = Wrappers.<Topic>lambdaUpdate()
                .eq(Topic::getId, topicId)
                .set(Topic::getUpdateTime, LocalDateTime.now())
                .setSql("like_count = like_count + 1");
        topicMapper.update(null, updateWrapper);
        return true;
    }

    @Override
    @Transactional
    public Boolean auditTopic(Long topicId, TopicAuditDTO dto) {
        if (dto == null || dto.getAdminId() == null) {
            throw new RuntimeException("管理员ID不能为空");
        }
        // 角色鉴权：验证是否为管理员
        User admin = userMapper.selectById(dto.getAdminId());
        if (admin == null || !"admin".equalsIgnoreCase(admin.getRole())) {
            throw new RuntimeException("仅管理员可执行审核操作");
        }

        Topic topic = topicMapper.selectById(topicId);
        if (topic == null || Boolean.TRUE.equals(topic.getDeleted())) {
            throw new RuntimeException("话题不存在");
        }
        if (!TopicStatusEnum.PENDING.name().equals(topic.getStatus())) {
            throw new RuntimeException("仅待审核的话题可以操作");
        }
        TopicStatusEnum decision = TopicStatusEnum.fromString(dto.getDecision());
        if (decision == null || TopicStatusEnum.PENDING.equals(decision)) {
            throw new RuntimeException("非法的审核状态");
        }
        topic.setStatus(decision.name());
        topic.setAuditBy(dto.getAdminId());
        topic.setAuditRemark(dto.getRemark());
        topic.setAuditTime(LocalDateTime.now());
        topic.setUpdateTime(LocalDateTime.now());
        topicMapper.updateById(topic);
        log.info("管理员[{}]审核话题[{}]结果为{}", dto.getAdminId(), topicId, decision.name());
        return true;
    }

    private Map<Long, List<TopicCommentVO>> fetchLatestComments(List<Long> topicIds) {
        if (CollectionUtils.isEmpty(topicIds)) {
            return Collections.emptyMap();
        }
        LambdaQueryWrapper<TopicComment> wrapper = Wrappers.<TopicComment>lambdaQuery()
                .in(TopicComment::getTopicId, topicIds)
                .orderByDesc(TopicComment::getCreateTime);
        List<TopicComment> comments = topicCommentMapper.selectList(wrapper);
        Map<Long, List<TopicCommentVO>> result = new HashMap<>();
        for (TopicComment comment : comments) {
            List<TopicCommentVO> commentList = result.computeIfAbsent(comment.getTopicId(), k -> new ArrayList<>());
            if (commentList.size() >= PREVIEW_COMMENT_LIMIT) {
                continue;
            }
            TopicCommentVO vo = new TopicCommentVO();
            BeanUtils.copyProperties(comment, vo);
            commentList.add(vo);
        }
        // 还原时间正序方便前端展示
        result.values().forEach(list -> list.sort(Comparator.comparing(TopicCommentVO::getCreateTime)));
        return result;
    }

    private String convertImagesToString(List<String> imageUrls) {
        if (CollectionUtils.isEmpty(imageUrls)) {
            return null;
        }
        return imageUrls.stream()
                .filter(StringUtils::hasText)
                .collect(Collectors.joining(","));
    }

    private List<String> convertImagesToList(String images) {
        if (!StringUtils.hasText(images)) {
            return Collections.emptyList();
        }
        return Arrays.stream(images.split(","))
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
    }

    @Override
    public TopicVO getTopicDetail(Long topicId) {

        Topic topic = topicMapper.selectById(topicId);
        if (topic == null || Boolean.TRUE.equals(topic.getDeleted())) {
            throw new RuntimeException("话题不存在");
        }

        TopicVO vo = new TopicVO();
        BeanUtils.copyProperties(topic, vo);
        vo.setImageUrls(convertImagesToList(topic.getImages()));

        return vo;
    }


    @Override
    public List<TopicCommentVO> listComments(Long topicId) {
        // 查询一级评论
        List<TopicComment> parentComments = topicCommentMapper.selectList(
                new LambdaQueryWrapper<TopicComment>()
                        .eq(TopicComment::getTopicId, topicId)
                        .isNull(TopicComment::getParentId)
                        .orderByAsc(TopicComment::getCreateTime)
        );

        return parentComments.stream()
                .map(this::convertWithReplies)
                .collect(Collectors.toList());
    }
    private TopicCommentVO convertWithReplies(TopicComment comment) {
        TopicCommentVO vo = convertToVO(comment);

        // 查询子评论
        List<TopicComment> replies = topicCommentMapper.selectList(
                new LambdaQueryWrapper<TopicComment>()
                        .eq(TopicComment::getParentId, comment.getId())
                        .orderByAsc(TopicComment::getCreateTime)
        );

        List<TopicCommentVO> replyVOs = replies.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        vo.setReplies(replyVOs);

        return vo;
    }

    private TopicCommentVO convertToVO(TopicComment comment) {
        TopicCommentVO vo = new TopicCommentVO();
        vo.setId(comment.getId());
        vo.setTopicId(comment.getTopicId());
        vo.setUserId(comment.getUserId());
        vo.setContent(comment.getContent());
        vo.setCreateTime(comment.getCreateTime());
        vo.setParentId(comment.getParentId());
        vo.setRootId(comment.getRootId());

        // 查询用户名
        User user = userMapper.selectById(comment.getUserId());
        if (user != null) {
            vo.setUsername(user.getUsername());
        } else {
            vo.setUsername("匿名");
        }
        if (vo.getUsername() == null) vo.setUsername("匿名");

        return vo;
    }
}

