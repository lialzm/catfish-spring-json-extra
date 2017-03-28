# catfish-spring-json-extra

> 在springmvc中的想要接收json格式报文,一般是使用@RequestBody映射到实体类或者HttpServletRequest自己读取处理,增加自定义参数处理器,用来更灵活的处理json



请求格式
```json
{
    "id": "11",
    "name": "hi"
}
```
```java
 @RequestMapping("/getParameterByDefaultKey")
    @ResponseBody
    public String getParameterByDefaultKey(@JsonRequest Long id, @JsonRequest String name) {
        return id + "," + name;
    }
```

或者
```java
    @RequestMapping("/getParameterByPath")
    @ResponseBody
    public String getParameterByPath(@JsonRequest("$.id") Long id, @JsonRequest("$.name") String name) {
        return id + "," + name;
    }
```
两种写法效果相同,value里可以定义其他jsonpath语法
[jsonpath使用方法](https://github.com/jayway/JsonPath)
支持默认参数
```java
 @RequestMapping("/getParameterByDefaultValue")
    @ResponseBody
    public String getParameterByDefaultValue(@JsonRequest(defaultValue = "0") Long id, @JsonRequest String name) {
        return id + "," + name;
    }
```
支持直接获取参数中包含的jsonobject
请求参数
```json
{
    "id": "11",
    "name": "qqq",
    "body": {
        "id": "222"
    }
}
```

```java
@RequestMapping("/getJsonObject")
    @ResponseBody
    public String getJsonObject(@JsonRequest JSONObjectWrapper body) {
        logger.info(body.toString());
        return body.getJSONObject().getString("id");
    }
```
支持获取全部json
```java
    @RequestMapping("/getAllJsonObject")
    @ResponseBody
    public String getAllJsonObject(@JsonRequest JSONWrapper name) {
        logger.info(name.toString());
        return name.getJSONObject().getString("id");
    }
```
支持将json中的jsonobject映射为bean
```java
@RequestMapping("/getBean")
    @ResponseBody
    public String getUserByJson7(@JsonRequest Body body) {
        return String.valueOf(body.getId());
    }
```
支持直接获取json中的jsonarray
```java
   @RequestMapping("/getJsonArray")
    @ResponseBody
    public String getJsonArray(@JsonRequest JSONArrayWrapper array) {
        JSONObject jsonObject = array.getJsonArray().getJSONObject(0);
        return jsonObject.getString("name");
    }
```
当然也支持多种参数混合获取
请求参数
```json
{
    "id": "11",
    "name": "qqq",
    "body": {
        "id": "222"
    },
    "array": [
        {
            "name": "item1"
        },
        {
            "name": "item2"
        }
    ]
}
```
```java
@RequestMapping("/getJsonArrayAndBean")
    @ResponseBody
    public String getJsonArrayAndBean(@JsonRequest JSONArrayWrapper array,@JsonRequest Body body) {
        JSONObject jsonObject = array.getJsonArray().getJSONObject(0);
        return jsonObject.getString("name")+","+body.getId();
    }
```