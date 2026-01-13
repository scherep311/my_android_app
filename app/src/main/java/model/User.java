package model;

public class User {
    public Long id;           // user_id
    public String firstName;
    public String lastName;
    public String patronymic;
    public String phoneNumber;
    public String role;
    public String email;
    public String avatarUrl;
    public Boolean isActive;

    // password на Android НЕ надо хранить/получать обычно,
    // но при регистрации сервер может ожидать role и т.д.
}
