package com.facelogin.model;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String username;
    
    @Column(nullable = false)
    private String email;
    
    @Lob
    @Column(name = "face_encoding", columnDefinition = "LONGTEXT")
    private String faceEncoding;
    
    @Column(name = "face_image_path")
    private String faceImagePath;
    
    
    @Column(name = "telegram_Chat_Id")
    private String telegramChatId; 
    
    public User() {}
    
    public User(String username, String email) {
        this.username = username;
        this.email = email;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getFaceEncoding() { return faceEncoding; }
    public void setFaceEncoding(String faceEncoding) { this.faceEncoding = faceEncoding; }
    
    public String getFaceImagePath() { return faceImagePath; }
    public void setFaceImagePath(String faceImagePath) { this.faceImagePath = faceImagePath; }

	public String getTelegramChatId() {
		return telegramChatId;
	}

	public void setTelegramChatId(String telegramChatId) {
		this.telegramChatId = telegramChatId;
	}
    
    
}
