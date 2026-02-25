package com.lsx.core.topic.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lsx.core.topic.entity.TopicComment;
import com.lsx.core.topic.vo.TopicCommentVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TopicCommentMapper extends BaseMapper<TopicComment> {
    @Select("SELECT * FROM biz_topic_comment WHERE topic_id = #{topicId}")
    List<TopicCommentVO> selectCommentsByTopicId(@Param("topicId") Long topicId);
}

