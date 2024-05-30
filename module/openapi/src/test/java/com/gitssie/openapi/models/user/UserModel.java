package com.gitssie.openapi.models.user;

import com.gitssie.openapi.ebean.GeneratorType;
import io.ebean.annotation.Index;
import io.ebean.annotation.*;

import javax.persistence.*;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "user")
public class UserModel {
    @Id
    @GeneratedValue(generator = GeneratorType.IdWorker)
    private Long id;
    protected Long entityType;
    @ManyToOne
    @JoinColumn(name = "owner_id")
    protected User owner;
    @ManyToOne
    @JoinColumn(name = "created_by")
    @WhoCreated
    protected User createdBy;
    @WhenCreated
    protected Date createdAt;
    @WhoModified
    @ManyToOne
    @JoinColumn(name = "updated_by")
    protected User updatedBy;
    @WhenModified
    protected Date updatedAt;
    @DbDefault(value = "0")
    protected int status; //通用状态字段
    @DbDefault(value = "false")
    protected boolean lockStatus;
    @Index(unique = true)
    private String username;//用户名
    private String phone;//手机号码phone电话类型
    private String name;//姓名name文本类型
    private String password; //密码
    private String personalEmail;//邮箱personalEmail邮箱类型;
    //部门dimDepart关联关系
    private String employeeCode;//员工编号employeeCode文本类型
    private String unionId;//唯一标识unionId文本类型
    private String uniqueKey;//唯一KeyuniqueKey文本类型
    private Date lastestLoginAt;

    @ManyToMany
    @JoinTable
    private List<UserRole> roles;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }


    //企业微信账号enterpriseWechatAccount文本类型
    //入职日期joinAt日期类型
    //出生日期birthday日期类型
    //密码规则passwordRuleId文本类型
    //隐藏年份标识hiddenYearFlg布尔类型
    //职位positionName文本类型
    //职级rankId整数类型
    //直属上级managerId关联关系
    //相关部门relatedDepart文本类型
    //语言编码languageCode文本类型
    //时区timezone文本类型
    //币种currency文本类型
    // 区域local文本类型
    //所属区域dimArea整数类型
    //相关区域relatedArea文本类型
    //所属业务dimBusiness整数类型
    //相关业务relatedBusiness文本类型
    // 所属产品线dimProduct整数类型
    //       相关产品relatedProduct文本类型
    //所属行业dimIndustry整数类型
    //        相关行业relatedIndustry文本类型
    //通讯录相关部门colleagueRelationDepart文本类型
    //       手机定位服务开户状态mobileLocationStatus布尔类型
    //昵称nickName文本类型
    //       状态status单选类型
    //是否虚拟用户isVirtual布尔类型
    //       最近一次登录时间lastestLoginAt日期时间类型
    //自我介绍selfIntro文本域类型
    //       办公电话telephone文本类型
    //分机号extNo文本类型
    //       业务专长expertise文本域类型
    //家乡hometown文本类型
    //QQ/MSNim文本类型
    //       微博weibo文本类型
    //兴趣爱好hobby文本域类型
    // post编码postCode文本类型

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPersonalEmail() {
        return personalEmail;
    }

    public void setPersonalEmail(String personalEmail) {
        this.personalEmail = personalEmail;
    }

    public String getEmployeeCode() {
        return employeeCode;
    }

    public void setEmployeeCode(String employeeCode) {
        this.employeeCode = employeeCode;
    }

    public String getUnionId() {
        return unionId;
    }

    public void setUnionId(String unionId) {
        this.unionId = unionId;
    }

    public String getUniqueKey() {
        return uniqueKey;
    }

    public void setUniqueKey(String uniqueKey) {
        this.uniqueKey = uniqueKey;
    }

    public List<UserRole> getRoles() {
        return roles;
    }

    public void setRoles(List<UserRole> roles) {
        this.roles = roles;
    }

    public Date getLastestLoginAt() {
        return lastestLoginAt;
    }

    public void setLastestLoginAt(Date lastestLoginAt) {
        this.lastestLoginAt = lastestLoginAt;
    }
}
