# Private-Cloud-Storage
支持分享、公，私有权限管理的个人云网盘

> 嫌弃Nginx自带文件服务器又不想用ownCloud的轮子    
> 基于Kotlin+SpringBoot+MongoDB开发
> 遵循RESTful规范   
> 前端网页版本由[Clovin](https://github.com/Clovin)完成，感谢Clovin大佬的测试与前端支持  
> 前端安卓版正在开发中（[Git Repo](https://github.com/LunziQwQ/private-cloud-storage-android)）    
> 欢迎大家批评指正  

`创建于2018年4月` 

---

## 说明
使用kotlin语言开发，基于SpringBoot+MongoDB，您需要拥有MongoDB以及Java8以上的运行环境  

数据库配置文件位于：`src/main/resources/application.properties`
日志配置文件位于：`src/main/resources/log4j.properties`

**可以脱离Tomcat环境，直接打包为Jar包，使用Maven的Package命令即可**  



## 功能介绍
* **文件上传、下载**：支持文件的上传下载，上传时通过文件内容MD5来防止重复源文件占用服务器空间。用户的文件列表仅保存索引项。  
* **文件的编辑**：支持重命名、删除、移动、更改权限、创建文件夹等操作，会自行检测重名文件冲突、是否拥有权限等常见问题，并返回对应的http状态码及问题描述。 
* **文件分享**：支持生成文件分享链接，允许他人直接访问并下载文件。分享链接可以控制有效时间。 
* **用户管理**：支持用户注册，用户空间限制，以及游客访问、用户访问自身空间，用户访问他人空间的权限分类。 

## API列表
API列表根据RESTful风格整理，以URL资源分类
* **session**
	* `POST /api/session` 用户登陆
		* 参数：`[x-www-form-urlencoded] username, password`
		* 成功返回：`200 OK {"result":true, "message":"Login success"}`
		* 失败返回：`403 FORBIDDEN {"result":false, "message":"Username or password wrong"}` 
	* `DELETE /api/session` 用户退出 
		* 返回：`200 OK {"result":true, "message":"Logout success"}` 
	* `GET /api/session` 获取当前登陆状态，当前登陆用户的权限详情
		* 未登录返回：`404 NOT_FOUND {"result":false, "message":"Permission denied. Maybe you are not login. Try to login first"}`
		* 已登录返回：`200 OK {"password":null, "username":"...", "authorities": [ { "authority": "..." } ],"accountNonExpired": true, "accountNonLocked": true, "credentialsNonExpired": true, "enabled": true}`
* **users**
	* `GET /api/users/{page}` 返回当前页数的用户列表（page可缺省，缺省时为第一页）
		* 返回范例：`200 OK [ {"username":"root", "userURL":"{Server address}/api/items/root", "admin":true}, ... ]`
* **user**
	* `GET /api/user/{username}` 返回该用户名的用户信息
		* 若用户存在返回：`200 OK {"isExist":true, "username":"...", "space":..., "index":"..."}`
		* 若用户不存在返回：`404 NOT_FOUND {"result":false, "message":"User not found"}`
	* `POST /api/user/{username}` 注册用户
		* 参数：`[json] {"password":"..."}`
		* 成功返回：`200 OK {"result":true, "message":"Register success"}`
		* 失败返回：`403 FORBIDDEN {"result":false, "message":"Username already used"}`
	* `DELETE /api/user/{username}` 删除用户
		* 参数：`[json] {password:"..."}`（用户自行删除需正确的密码，管理员传递空参数""即可）
		* 成功返回： `200 OK {"result":true, "message":"Delete user and ... file items success"}`
		* 失败返回：`404 NOT_FOUND {"result":false, "message":"..."}`
	* `PUT /api/user/{username}/space` 修改用户可用空间大小（仅管理员）
		* 参数：`[json] {"space": 256000}`
		* 成功返回：`200 OK {"result":true, "message":"Change ... space to 256000"}`
		* 失败返回：`403 FORBIDDEN {"result":false, "message":"..."}`		
	* `PUT /api/user/{username}/password` 修改用户密码
		* 参数：`[json] {"oldPassword": "...", "newPassword": "..."}` 若管理员oldPassword直接传递空`"oldPassword": ""`即可，用户需要传递正确的旧密码。
		* 成功返回：`200 OK {"result":true, "message":"Change ... password success"}`
		* 失败返回：`403 FORBIDDEN {"result":false, "message":"..."}`
* **file**
	* `GET /api/file/{completePath}` 下载文件（completePath为包含文件名的完整路径）
		* 成功返回：`[application/octet-stream]` 文件流
		* 失败返回：`404 NOT_FOUND {"result":false, "message":"..."}`
	* `POST /api/file/{path}` 上传文件(Path 为上传的目标目录路径，不包含待上传文件的文件名)
		* 参数：`[form-data] file: <Files>` 可同时上传多个文件
		* 返回：`200 OK [ {result: <true/false>, message: "..."}, ...]` 根据文件数目返回结果列表（无论成功失败状态码均为200）
* **items**
	* `GET /api/items/{CompletePath}` 获取该目录下的所有item，CompletePath为想查询的目录，用户的根目录均为`username/`。如`GET /api/items/root/`为获取root用户的根目录。
		* 成功返回：`200 OK [ {"itemName":"...", "path":"...", "size":"...", "lastModified":"<ISO-8601>yyyy-mm-ddThh:mm:ss[.mmm]", "public":<true/flase>, "dictionary":<true/false>}, ... ]`
		* 失败返回：`404 NOT_FOUND {"result":false, "message":"..."}`
	* `POST /api/items/{newPath}`  转存文件，将他人文件转存到自己空间，newPath为想要转存到的自己空间的目录路径		
		* 参数：`[json] {"path":"...", "name":"..."}` path为想要转存的文件路径（不包括文件名），name为想要转存的文件名。
		* 成功返回：`200 OK {"result":true, "message":"Transfer ... to ... total 4 items success"}`
		* 失败返回：`404 NOT_FOUND {"result":false, "message":"..."}`
* **item**
	* `POST /api/item/{CompletePath}`  创建文件夹
		* 成功返回：`200 OK {"result":true, "message":"Create dictionary ... success"}`
		* 失败返回：`404 NOT_FOUND {"result":false, "message":"..."}`
	* `DELETE /api/item/{CompletePath}`  文件删除
		* 成功返回：`200 OK {"result":true, "message":"Delete folder ... total 4 items success"}`
		* 失败返回：`404 NOT_FOUND {"result":false, "message":"..."}`
	* `PUT /api/item/{CompletePath}/name`  文件重命名
		* 参数：`[json] {"newName":"..."}` 要更改的新名字
		* 成功返回：`200 OK {"result":true, "message":"Rename ... to ... success"}`
		* 失败返回：`404 NOT_FOUND {"result":false, "message":"..."}`
	* `PUT /api/item/{CompletePath}/path` 文件移动
		* 参数：`[json] {"newPath": "..."}` 新的目标路径
		* 成功返回：`200 OK {"result":true, "message":"Move ... to ... total 4 items success"}`
		* 失败返回：`400 BAD_REQUEST {"result":false, "message":"..."}`
	* `PUT /api/item/{CompletePath}/access` 更改文件权限
		* 参数：`[json] {"isPublic":<true/false>, "allowRecursion":<true/false>}` isPublic为要更改的目标权限，allowRecurision为是否要递归更改子文件的权限。
		* 成功返回：`200 OK {"result":true, "message":"Change ... total 4 items access to ... success"}`
		* 失败返回：`404 NOT_FOUND {"result":false, "message":"..."}`
