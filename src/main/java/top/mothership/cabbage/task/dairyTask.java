package top.mothership.cabbage.task;

import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;
import top.mothership.cabbage.mapper.BaseMapper;
import top.mothership.cabbage.pojo.Userinfo;
import top.mothership.cabbage.util.ApiUtil;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.sql.Date;
import java.util.*;

@Component
public class dairyTask {
    private Logger logger = LogManager.getLogger(this.getClass());
    private BaseMapper baseMapper;
    private ApiUtil apiUtil;
    private final JavaMailSender javaMailSender;
    private final FreeMarkerConfigurer freeMarkerConfigurer;
    private java.util.Date start;
    @Autowired
    public dairyTask(BaseMapper baseMapper, ApiUtil apiUtil, JavaMailSender javaMailSender, FreeMarkerConfigurer freeMarkerConfigurer) {
        this.baseMapper = baseMapper;
        this.apiUtil = apiUtil;
        this.javaMailSender = javaMailSender;
        this.freeMarkerConfigurer = freeMarkerConfigurer;
    }

    @Scheduled(cron = "0 58 10 * * ?")
    public void importUserInfo() {
        start = Calendar.getInstance().getTime();
        Calendar cl = Calendar.getInstance();
        cl.add(Calendar.DATE,-1);
        baseMapper.clearTodayInfo(new Date(cl.getTimeInMillis()));
        logger.info("开始进行每日登记");
        List<Integer> list = baseMapper.listUserIdByRole(null);
        List<Integer> nullList = new ArrayList<>();
        for (Integer aList : list) {
            Userinfo userinfo = apiUtil.getUser(null,String.valueOf(aList));
            if (userinfo != null) {
                //将日期改为一天前写入
                userinfo.setQueryDate(new java.sql.Date(Calendar.getInstance().getTimeInMillis() - 1000 * 3600 * 24));
                baseMapper.addUserInfo(userinfo);
                logger.info("将" + userinfo.getUserName() + "的数据录入成功");
            } else {
                nullList.add(aList);
            }
        }
        logger.info("以下玩家没有在官网抓取到数据，正在发送邮件：" + nullList );
        Map<String,String> map = new HashMap<>();
        map.put("nullList",nullList.toString());
        sendMail("1335734657@qq.com",map);
        sendMail("2307282906@qq.com",map);
        logger.info("处理完毕，共耗费" + (Calendar.getInstance().getTimeInMillis() - start.getTime()) + "ms。");
    }
    private void sendMail(String target, Map<String,String> map) {
        String content;
        Template template = null;
        try {
            template = freeMarkerConfigurer.getConfiguration().getTemplate("mail.ftl");
            content = FreeMarkerTemplateUtils.processTemplateIntoString(template, map);
            //创建多媒体邮件
            MimeMessage mmm = javaMailSender.createMimeMessage();
            //修改邮件体
            MimeMessageHelper mmh = new MimeMessageHelper(mmm, true, "UTF-8");
            //设置发件人信息
            mmh.setFrom("1335734657@qq.com");
            //收件人
            mmh.setTo(target);
            //主题
            mmh.setSubject("数据录入结果通知");
            //内容
            mmh.setText(content, true);
            //送出
            javaMailSender.send(mmm);
        } catch (MessagingException|IOException | TemplateException e) {
            e.printStackTrace();
        }


    }
}