package com.aoe4Forum.web.controller;

import com.aoe4Forum.entity.Comment;
import com.aoe4Forum.entity.Post;
import com.aoe4Forum.entity.PostContent;
import com.aoe4Forum.entity.ResponseVO;
import com.aoe4Forum.entity.request.CommentRequest;
import com.aoe4Forum.entity.request.CreatePostRequest;
import com.aoe4Forum.entity.request.PostRequest;
import com.aoe4Forum.entity.request.QueryPostRequest;
import com.aoe4Forum.service.CommentService;
import com.aoe4Forum.service.impl.CommentServiceImpl;
import com.aoe4Forum.service.impl.PostServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@CrossOrigin
@RestController
@RequestMapping("/community")
@Validated
public class CommunityController extends ABaseController{

    @Resource
    private PostServiceImpl postServiceImpl;

    @Autowired
    private CommentService commentService;

    //和帖子相关的api
    @PostMapping("/createPost")
    public ResponseVO<Map<String,String>> createPost(@Valid @RequestBody CreatePostRequest createPostRequest,
                                                     HttpServletRequest request
                                                     ){
        setRequestParams(request,createPostRequest);
//       插入帖子
        try{
            postServiceImpl.createPost(createPostRequest);
        }catch (Exception e){
            e.printStackTrace();
        }

        return ResponseVO.success("发帖成功",null);
    }

    @PostMapping("/deletePost")
    public ResponseVO<Map<String,String>> deletePost(@Valid @RequestBody PostRequest postRequest,
                                                     HttpServletRequest request
                                                     ){
        setRequestParams(request,postRequest);
//        删除帖子
        postServiceImpl.deletePost(postRequest);
        return ResponseVO.success("删除成功",null);
    }

    @PostMapping("/updatePost")
    public ResponseVO<Map<String,String>> updatePost(@Valid @RequestBody PostRequest postRequest,
                                                     HttpServletRequest request
                                                     ){
//        TODO 需要加一个拦截器做登录
        setRequestParams(request,postRequest);
//       修改帖子
        postServiceImpl.updatePost(postRequest);
        return ResponseVO.success("更新成功",null);
    }

//    查询指定数量的帖子信息，不包含具体内容
    @PostMapping("/queryPostByForum")
    public ResponseVO<List<Post>> queryPostByForum(@Valid @RequestBody QueryPostRequest queryPostRequest){

        List<Post> posts = postServiceImpl.queryPostByForum(queryPostRequest);

        return  ResponseVO.success("查询成功",posts);
    }
    @RequestMapping("/queryPostById")
    public ResponseVO<Post> queryPostById(@RequestParam Long postId){
        Post post = postServiceImpl.queryPostById(postId);
        return  ResponseVO.success("查询成功",post);
    }
//    查询指定数量的帖子信息，不包含具体内容
    @PostMapping("/queryPostByHot")
    public ResponseVO<List<Post>> queryPostByHot(@Valid @RequestBody QueryPostRequest queryPostRequest){

        List<Post> posts = postServiceImpl.queryPostByHot(queryPostRequest);

        return  ResponseVO.success("查询成功",posts);
    }

    @RequestMapping("/queryPostContent")
    public ResponseVO<PostContent> queryPostContent(long postId){
        PostContent postcontent = postServiceImpl.queryPostContent(postId);

        return  ResponseVO.success("查询成功",postcontent);
    }

    //    评论api
    @Resource
    CommentServiceImpl commentServiceImpl;

    @PostMapping("/createComment")
    public ResponseVO<Map<String,String>> CreateComment(@Valid @RequestBody CommentRequest commentRequest,
                                                        HttpServletRequest request
                                                        ){
        setRequestParams(request,commentRequest);
        commentServiceImpl.createComment(commentRequest);
        return ResponseVO.success("评论成功",null);
    }

    @RequestMapping("/deleteComment")
    public ResponseVO<Map<String,String>> deleteComment(@RequestParam Long commentId,
                                                        HttpServletRequest request
                                                        ){
        CommentRequest commentRequest = new CommentRequest();
        commentRequest.setCommentId(commentId);
        setRequestParams(request,commentRequest);
        commentServiceImpl.deleteComment(commentRequest);
        return ResponseVO.success("删除成功",null);
    }

    @RequestMapping("/getComment")
    public ResponseVO<Comment> getComment(@RequestParam Long commentId){
        Comment comment = commentService.getComment(commentId);
        return ResponseVO.success("查询成功",comment);
    }


    @RequestMapping("/getCommentsByPostId")
    public ResponseVO<List<Comment>> getCommentsByPostId(@RequestParam Long postId,
                                                         @RequestParam int offset,
                                                         @RequestParam int limit
                                                         ){
//       先查评论，之后根据评论查3条最早发布的子评论。
        List<Comment> parentComments = commentServiceImpl.getCommentsByPostId(postId,offset,limit);
        if(parentComments.isEmpty()){
            return ResponseVO.error("103","查询失败，没有评论");
        }
        return   ResponseVO.success("查询成功",parentComments);
    }

    @RequestMapping("getCommentsByParentId")
    public ResponseVO<List<Comment>> getCommentsByParentId(@RequestParam Long parentId,
                                                           @RequestParam int offset,
                                                           @RequestParam int limit
                                                           ){
        List<Comment> childComments = commentServiceImpl.getCommentsByParentId(parentId,offset,limit);
        if(childComments.isEmpty()){
            return ResponseVO.error("103","查询失败，没有评论");
        }
        return ResponseVO.success("查询成功",childComments);
    }

    @PostMapping("getCommentsByParentIds")
    public ResponseVO<Map<Long,List<Comment>>> getCommentsByParentIds(@RequestBody List<Long> parentIds
                                                                     ){
        if(parentIds.isEmpty()){
            return ResponseVO.error("103","查询失败，没有需要查询的评论");
        }
        Map<Long,List<Comment>> result = commentServiceImpl.getCommentsByParentIds(parentIds,3);
        if(result==null||result.isEmpty()){
            return ResponseVO.error("103","查询失败，没有子评论");
        }
        return ResponseVO.success("查询成功",result);
    }

}
