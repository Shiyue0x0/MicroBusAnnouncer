<img align="left" src='app/src/main/res/mipmap-hdpi/an_round.png' width='100px'>
<br/>

# Announcer

[![Latest release](https://img.shields.io/github/v/release/Shiyue0x0/MicroBusAnnouncer?label=Release&logo=github)](https://github.com/Shiyue0x0/MicroBusAnnouncer/releases/latest)

### 这是什么？

`Announcer`是一款Android公交报站器，集成了 `站点与路线管理` `语音播报` `运行信息与地图实况` `自定义模拟电显` 等功能。

![UI预览](readme/img/uiPreview.webp)

相关视频：

[发布视频：【自由定制的公交报站器】附桂林公交AI流萤语音库](https://www.bilibili.com/video/BV1sdn7zhEQo)

[使用视频：桂林公交K2路 自制报站 全程运行示意](https://www.bilibili.com/video/BV1mE9uBzE5H)


1. 站点与路线管理

- 自定义站点与路线
- 可以导入高德地图中的公交/地铁路线

2. 语音播报

- 自定义播报文本内容
- 自定义播报音频资源
- 可调用系统TTS文字转语音

3. 运行信息与地图实况

- 运行点屏（站点列表）
- 当前站点/距离/速度显示
- 地图站点/路线轨迹显示

4. 自定义模拟电显

- 自定义电显内容
- 可显示多语言站点名称、速度、时间等
- 动态切换，可根据当前路线运行状态显示不同文本


### 快速开始

无需过多配置，只需要一些步骤即可体验Announcer的基本功能。

1. 准备一台Android 8.0+的手机，从 `GitHub` 或 `Gitee` 下载最新版 `应用本体` 和 `体验语音库` ：

   GitHub: [![Latest release](https://img.shields.io/github/v/release/Shiyue0x0/MicroBusAnnouncer?label=Release&logo=github)](https://github.com/Shiyue0x0/MicroBusAnnouncer/releases/latest)

   Gitee: [Gitee](https://gitee.com/shiyue0x0/micro-bus-announcer/releases/latest)

```text
   语音来源：
   Firefly：《崩坏：星穹铁道》流萤
   桂林公交：`@ZK6858HAA` `@机场闪电359`
   鸣谢：
   GPT-SoVITS开发者：`@花儿不哭`
   模型训练者：`@红血球AE3803 &` `@白菜工厂1145号员工`
   推理特化包适配 & 在线推理：`@AI-Hobbyist`
```

2. 将体验语音库的 `Announcer` 文件夹解压到手机的根目录。

3. 安装并启动应用，并授予所需的权限。

4. 点击右下角的 `设置`-`数据与关于` ，点击 `加载预设数据` `站点和路线`。


喜报！您已经完成了所有的初始设置。现在可以试着自行探索一下，或者是继续跟随文档操作。

### 开始运行

1. 现在，来试着运行路线。请先点击左下角的 `主控`，接下来大部分的操作会在 `主控` 页进行。

2. 点击最上方的卡片 `模拟电显` ，然后选择要运行的路线。也可以点击`线路`页中更换路线。

3. 此时，您选择的路线已经开始运行。`Announcer`在前台运行时会根据实时定位，在进站和出站时进行自动报站。

4. 如果您的定位没有改变，就无法触发自动报站。虽然但是，您仍可以点击界面下方中央类似铃铛的报站按钮，来试听报站。

5. 点击电显下方路线运行图任意一个站点，然后点击`当前站点`按钮，就会切换到这一站，再次点击报站按钮试听当前站点的报站。

要继续了解`Announcer`？请按需参阅这些文档：

1.[语音播报](readme/语音播报.md)

2.[模拟电显](readme/模拟电显.md)

3.[自定义站点与路线](readme/自定义站点与路线.md)

4.[环线](readme/环线.md)

