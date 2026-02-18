package com.englishword.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "搜索单词结果")
public class WordResult {

    @Schema(description = "单词拼写", example = "ephemeral")
    private String word;

    @Schema(description = "音标", example = "/ɪˈfem(ə)rəl/")
    private String phonetic;

    @Schema(description = "词性", example = "adj.")
    private String partOfSpeech;

    @Schema(description = "中文释义", example = "短暂的;转瞬即逝的")
    private String meaning;

    @Schema(description = "例句", example = "Fame is ephemeral.")
    private String example;
}
