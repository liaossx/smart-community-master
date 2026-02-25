package com.lsx.core.topic.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lsx.core.topic.dto.TopicAuditDTO;
import com.lsx.core.topic.dto.TopicCommentDTO;
import com.lsx.core.topic.dto.TopicCreateDTO;
import com.lsx.core.topic.entity.Topic;
import com.lsx.core.topic.vo.TopicCommentVO;
import com.lsx.core.topic.vo.TopicVO;

import java.util.List;

public interface TopicService extends IService<Topic> {
    Long createTopic(TopicCreateDTO dto);

    Page<TopicVO> pageTopics(Integer pageNum, Integer pageSize, String status);

    Long addComment(Long topicId, TopicCommentDTO dto);

    Boolean likeTopic(Long topicId, Long userId);

    Boolean auditTopic(Long topicId, TopicAuditDTO dto);

    TopicVO getTopicDetail(Long topicId);


    List<TopicCommentVO> listComments(Long topicId);


}

