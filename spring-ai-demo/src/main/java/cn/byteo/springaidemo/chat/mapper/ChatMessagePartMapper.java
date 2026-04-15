package cn.byteo.springaidemo.chat.mapper;

import cn.byteo.springaidemo.chat.entity.ChatMessagePart;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatMessagePartMapper extends BaseMapper<ChatMessagePart> {
}
