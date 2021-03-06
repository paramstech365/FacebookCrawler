package api.facebook.bean;

import java.sql.Timestamp;

/**
 * Posts entity. @author MyEclipse Persistence Tools
 */

public class Posts
{

	// Fields
	//这个属性用于标示返回的posts 的json数据的状态，error表示出现错误；empty表示已经爬取到当前的尽头（无论是历史数据还是未来数据）
	private String status="";
	private String codeMessage="";
	
	private Integer postId;
	private Integer seedsId;
	private String messageId;
	private String message;
	private Timestamp createdTime;
	private Timestamp insertTime;
	private String link;
	private String picture;
	private Integer shares;

	private String postsPreviousPage;
	private String postsNextPage;
	
	private String commentsPreviousPage;
	private String commentsNextPage;
	private String likesPreviousPage;
	private String likesNextPage;
	// Constructors

	/** default constructor */
	public Posts() {
	}

	/** minimal constructor */
	public Posts(String messageId, Timestamp createdTime,
			Timestamp insertTime) {
		this.messageId = messageId;
		this.createdTime = createdTime;
		this.insertTime = insertTime;
	}

	/** full constructor */
	public Posts(String messageId, String message,
			Timestamp createdTime, Timestamp insertTime, String link,
			String picture, Integer shares) {
		this.messageId = messageId;
		this.message = message;
		this.createdTime = createdTime;
		this.insertTime = insertTime;
		this.link = link;
		this.picture = picture;
		this.shares = shares;
	}

	// Property accessors

	public Integer getPostId() {
		return this.postId;
	}

	public void setPostId(Integer postId) {
		this.postId = postId;
	}


	public Integer getSeedsId() {
		return seedsId;
	}

	public void setSeedsId(Integer seedsId) {
		this.seedsId = seedsId;
	}

	public String getMessageId() {
		return this.messageId;
	}

	public void setMessageId(String messageId) {
		this.messageId = messageId;
	}

	public String getMessage() {
		return this.message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public Timestamp getCreatedTime() {
		return this.createdTime;
	}

	public void setCreatedTime(Timestamp createdTime) {
		this.createdTime = createdTime;
	}

	public Timestamp getInsertTime() {
		return this.insertTime;
	}

	public void setInsertTime(Timestamp insertTime) {
		this.insertTime = insertTime;
	}

	public String getLink() {
		return this.link;
	}

	public void setLink(String link) {
		this.link = link;
	}

	public String getPicture() {
		return this.picture;
	}

	public void setPicture(String picture) {
		this.picture = picture;
	}

	public Integer getShares() {
		return this.shares;
	}

	public void setShares(Integer shares) {
		this.shares = shares;
	}

	

	public String getPostsPreviousPage() {
		return postsPreviousPage;
	}

	public void setPostsPreviousPage(String postsPreviousPage) {
		this.postsPreviousPage = postsPreviousPage;
	}

	public String getPostsNextPage() {
		return postsNextPage;
	}

	public void setPostsNextPage(String postsNextPage) {
		this.postsNextPage = postsNextPage;
	}


	

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getCodeMessage() {
		return codeMessage;
	}

	public void setCodeMessage(String codeMessage) {
		this.codeMessage = codeMessage;
	}

	public String getCommentsPreviousPage() {
		return commentsPreviousPage;
	}

	public void setCommentsPreviousPage(String commentsPreviousPage) {
		this.commentsPreviousPage = commentsPreviousPage;
	}

	public String getCommentsNextPage() {
		return commentsNextPage;
	}

	public void setCommentsNextPage(String commentsNextPage) {
		this.commentsNextPage = commentsNextPage;
	}

	public String getLikesPreviousPage() {
		return likesPreviousPage;
	}

	public void setLikesPreviousPage(String likesPreviousPage) {
		this.likesPreviousPage = likesPreviousPage;
	}

	public String getLikesNextPage() {
		return likesNextPage;
	}

	public void setLikesNextPage(String likesNextPage) {
		this.likesNextPage = likesNextPage;
	}

}