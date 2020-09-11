package com.lyp.controller;

import com.github.pagehelper.PageInfo;
import com.vdurmont.emoji.EmojiParser;
import com.lyp.constant.ErrorConstant;
import com.lyp.constant.Types;
import com.lyp.constant.WebConst;
import com.lyp.dto.MetaDto;
import com.lyp.dto.cond.ContentCond;
import com.lyp.exception.BusinessException;
import com.lyp.model.CommentDomain;
import com.lyp.model.ContentDomain;
import com.lyp.model.MetaDomain;
import com.lyp.service.article.ContentService;
import com.lyp.service.comment.CommentService;
import com.lyp.service.meta.MetaService;
import com.lyp.utils.APIResponse;
import com.lyp.utils.IPKit;
import com.lyp.utils.TaleUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.util.List;

/**
 * @author 你鹏哥
 * 主要为前台博客页面的·控制层
 */
@Api("博客前台页面")
@Controller
public class HomeController extends BaseController {

    @Autowired
    private ContentService contentService;

    @Autowired
    private CommentService commentService;

    @Autowired
    private MetaService metaService;


    /**
     * 分页
     * @param request
     * @param page
     * @param limit
     * @return
     */
    @GetMapping(value = "/")
    public String index(
            HttpServletRequest request,
            @ApiParam(name = "page", value = "页数", required = false)
            @RequestParam(name = "page", required = false, defaultValue = "1")
            int page,
            @ApiParam(name = "limit", value = "每页数量", required = false)
            @RequestParam(name = "limit", required = false, defaultValue = "5")
            int limit
    ) {
        // 调用业务层，查询数据库
        PageInfo<ContentDomain> articles = contentService.getArticlesByCond(new ContentCond(), page, limit);

        // 发送数据到前台页面
        request.setAttribute("articles",articles);
        return "blog/home";
    }

    /**
     * 归档内容叶
     * @param request
     * @param page
     * @param limit
     * @return
     */
    @ApiOperation("归档内容页")
    @GetMapping(value = "/archives")
    public String archives(
            HttpServletRequest request,
            @ApiParam(name = "page", value = "页数", required = false)
            @RequestParam(name = "page", required = false, defaultValue = "1")
            int page,
            @ApiParam(name = "limit", value = "每页数量", required = false)
            @RequestParam(name = "limit", required = false, defaultValue = "10")
            int limit
    ) {
        PageInfo<ContentDomain> articles = contentService.getArticlesByCond(new ContentCond(), page, limit);
        request.setAttribute("articles", articles);
        return "blog/archives";
    }

    /**
     *
     * @param request
     * @return
     */
    @ApiOperation("分类内容页")
    @GetMapping(value = "/categories")
    public String categories(HttpServletRequest request) {
        // 获取分类 /WebConst.MAX_POSTS: 最大获取文章条数
        List<MetaDto> categories = metaService.getMetaList(Types.CATEGORY.getType(),null,WebConst.MAX_POSTS);
        // 分类总数
        Long categoryCount = metaService.getMetasCountByType(Types.CATEGORY.getType());
        request.setAttribute("categories", categories);
        request.setAttribute("categoryCount", categoryCount);
        return "blog/category";
    }

    @ApiOperation("分类详情页")
    @GetMapping(value = "/categories/{name}")
    public String categoriesDetail(
            HttpServletRequest request,
            @ApiParam(name = "name", value = "分类名称", required = true)
            @PathVariable("name")
            String name
    ) {
        MetaDomain category = metaService.getMetaByName(Types.CATEGORY.getType(),name);
        if (null == category.getName())
            throw BusinessException.withErrorCode(ErrorConstant.Common.PARAM_IS_EMPTY);
        List<ContentDomain> articles = contentService.getArticleByCategory(category.getName());
        request.setAttribute("category", category.getName());
        request.setAttribute("articles", articles);
        return "blog/category_detail";
    }

    @ApiOperation("标签内容页")
    @GetMapping(value = "/tags")
    public String tags(HttpServletRequest request) {
        // 获取标签
        List<MetaDto> tags = metaService.getMetaList(Types.TAG.getType(), null, WebConst.MAX_POSTS);
        // 标签总数
        Long tagCount = metaService.getMetasCountByType(Types.TAG.getType());
        request.setAttribute("tags", tags);
        request.setAttribute("tagCount", tagCount);
        return "blog/tags";
    }

    @ApiOperation("标签详情页")
    @GetMapping(value = "/tags/{name}")
    public String tagsDetail(
            HttpServletRequest request,
            @ApiParam(name = "name", value = "标签名", required = true)
            @PathVariable("name")
            String name
    ) {
        MetaDomain tags = metaService.getMetaByName(Types.TAG.getType(),name);
        List<ContentDomain> articles = contentService.getArticleByTags(tags);
        request.setAttribute("articles",articles);
        request.setAttribute("tags",tags.getName());
        return "blog/tags_detail";
    }

    @GetMapping(value = "/about")
    public String about() {
        return "blog/about";
    }

    @ApiOperation("文章内容页")
    @GetMapping(value = "/detail/{cid}")
    public String detail(
            @ApiParam(name = "cid", value = "文章主键", required = true)
            @PathVariable("cid")
            Integer cid,
            HttpServletRequest request
    ) {
        ContentDomain article = contentService.getArticleById(cid);
        request.setAttribute("article", article);

        // 更新文章的点击量
        this.updateArticleHits(article.getCid(),article.getHits());
        // 获取评论
        List<CommentDomain> comments = commentService.getCommentsByCId(cid);
        request.setAttribute("comments", comments);

        return "blog/detail";
    }

    /**
     * 更新文章的点击率
     * @param cid
     * @param chits
     */
    private void updateArticleHits(Integer cid, Integer chits) {
        Integer hits = cache.hget("article", "hits");
        if (chits == null) {
            chits = 0;
        }
        hits = null == hits ? 1 : hits + 1;
        if (hits <= WebConst.HIT_EXEED) {
            ContentDomain temp = new ContentDomain();
            temp.setCid(cid);
            temp.setHits(chits + hits);
            contentService.updateContentByCid(temp);
            cache.hset("article", "hits", 1);
        } else {
            cache.hset("article", "hits", hits);
        }

    }

    @RequestMapping(value = "/comment")
    @ResponseBody
    public APIResponse comment(HttpServletRequest request, HttpServletResponse response,
                               @RequestParam(name = "cid", required = true) Integer cid,
                               @RequestParam(name = "coid", required = true) Integer coid,
                               @RequestParam(name = "author", required = true) String author,
                               @RequestParam(name = "email", required = true) String email,
                               @RequestParam(name = "url", required = true) String url,
                               @RequestParam(name = "content", required = true) String content,
                               @RequestParam(name = "csrf_token", required = true) String csrf_token
                               ) {
     /*
        System.out.println(cid);
        System.out.println(coid);
        System.out.println(author);
        System.out.println(email);
        System.out.println(url);
        System.out.println(content);
        System.out.println(csrf_token);*/




        String ref = request.getHeader("Referer");
        if (StringUtils.isBlank(ref) || StringUtils.isBlank(csrf_token)){
            return APIResponse.fail("访问失败");
        }

        String token = cache.hget(Types.CSRF_TOKEN.getType(), csrf_token);
        if (StringUtils.isBlank(token)) {
            return APIResponse.fail("访问失败");
        }

        if (null == cid || StringUtils.isBlank(content)) {
            return APIResponse.fail("请输入完整后评论");
        }

        if (StringUtils.isNotBlank(author) && author.length() > 50) {
            return APIResponse.fail("姓名过长");
        }

        if (StringUtils.isNotBlank(email) && !TaleUtils.isEmail(email)) {
            return APIResponse.fail("请输入正确的邮箱格式");
        }

        if (StringUtils.isNotBlank(url) && !TaleUtils.isURL(url)) {
            return APIResponse.fail("请输入正确的网址格式");
        }

        if (content.length() > 200) {
            return APIResponse.fail("请输入200个字符以内的评价");
        }

        String val = IPKit.getIpAddressByRequest1(request) + ":" + cid;
        Integer count = cache.hget(Types.COMMENTS_FREQUENCY.getType(), val);
        if (null != count && count > 0) {
            return APIResponse.fail("您发表的评论太快了，请过会再试");
        }

        author = TaleUtils.cleanXSS(author);
        content = TaleUtils.cleanXSS(content);

        author = EmojiParser.parseToAliases(author);
        content = EmojiParser.parseToAliases(content);


        CommentDomain comments = new CommentDomain();
        comments.setAuthor(author);
        comments.setCid(cid);
        comments.setIp(request.getRemoteAddr());
        comments.setUrl(url);
        comments.setContent(content);
        comments.setEmail(email);
        comments.setParent(coid);

        try {
            commentService.addComment(comments);
            cookie("tale_remember_author", URLEncoder.encode(author,"UTF-8"), 7 * 24 * 60 * 60, response);
            cookie("tale_remember_mail", URLEncoder.encode(email,"UTF-8"), 7 * 24 * 60 * 60, response);
            if (StringUtils.isNotBlank(url)) {
                cookie("tale_remember_url",URLEncoder.encode(url,"UTF-8"),7 * 24 * 60 * 60, response);
            }
            // 设置对每个文章1分钟可以评论一次
            cache.hset(Types.COMMENTS_FREQUENCY.getType(),val,1,60);

            return APIResponse.success();

        } catch (Exception e) {
            throw BusinessException.withErrorCode(ErrorConstant.Comment.ADD_NEW_COMMENT_FAIL);
        }


    }

    private void cookie(String name, String value, int maxAge, HttpServletResponse response) {
        Cookie cookie = new Cookie(name,value);
        cookie.setMaxAge(maxAge);
        cookie.setSecure(false);

        response.addCookie(cookie);
    }


}
