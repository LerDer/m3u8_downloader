package com.ler.controller;

import com.ler.utils.CommonUtil;
import com.ler.utils.M3U8Utils;
import com.ler.vo.HttpResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author lww
 */
@Slf4j
@RestController
@RequestMapping("/m3u8")
@Api(value = "/m3u8", description = "m3u8下载器")
public class M3U8Controller {

    @ApiOperation("我的下载一个")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "m3u8Url", value = "m3u8链接", required = true),
            @ApiImplicitParam(name = "path", value = "存储路径", required = true),
            @ApiImplicitParam(name = "fileName", value = "保存的文件名字", required = true),
    })
    @PostMapping(value = "/my/one", name = "我的下载一个")
    public HttpResult myOne(String m3u8Url, String path, String fileName) throws Exception {
        long start = System.currentTimeMillis();
        CommonUtil.isTrue(StringUtils.isNotBlank(m3u8Url), "m3u8链接不能为空！");
        CommonUtil.isTrue(StringUtils.isNotBlank(path), "存储路径不能为空！");
        CommonUtil.isTrue(StringUtils.isNotBlank(fileName), "文件名不能为空！");
        M3U8Utils.download(m3u8Url.trim(), path.trim(), fileName.trim());
        long times = System.currentTimeMillis() - start;
        String s = CommonUtil.second2Time((double) (times / 1000));
        log.info("已完成,耗时: {}", s);
        return HttpResult.success("已完成,耗时" + s);
    }

}
