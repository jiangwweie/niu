package com.xiaoniu.aftermarket.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SearchKeywordUtilsTest {

    @Test
    void normalizeReturnsNullForBlankValues() {
        assertThat(SearchKeywordUtils.normalize(null)).isNull();
        assertThat(SearchKeywordUtils.normalize("")).isNull();
        assertThat(SearchKeywordUtils.normalize("   ")).isNull();
    }

    @Test
    void normalizeTrimsSearchText() {
        assertThat(SearchKeywordUtils.normalize(" abc ")).isEqualTo("abc");
        assertThat(SearchKeywordUtils.normalize(" 小牛配件 ")).isEqualTo("小牛配件");
        assertThat(SearchKeywordUtils.normalize(" 1388 ")).isEqualTo("1388");
        assertThat(SearchKeywordUtils.normalize(" LZXTDJ123456 ")).isEqualTo("LZXTDJ123456");
        assertThat(SearchKeywordUtils.normalize(" nQi-AB12 ")).isEqualTo("nQi-AB12");
    }

    @Test
    void escapeLikeEscapesWildcardAndEscapeCharacters() {
        assertThat(SearchKeywordUtils.escapeLike("%")).isEqualTo("!%");
        assertThat(SearchKeywordUtils.escapeLike("_")).isEqualTo("!_");
        assertThat(SearchKeywordUtils.escapeLike("\\")).isEqualTo("!\\");
        assertThat(SearchKeywordUtils.escapeLike("20%_\\")).isEqualTo("20!%!_!\\");
    }

    @Test
    void buildContainsPatternWrapsEscapedKeyword() {
        assertThat(SearchKeywordUtils.buildContainsPattern("abc")).isEqualTo("%abc%");
        assertThat(SearchKeywordUtils.buildContainsPattern(" % ")).isEqualTo("%!%%");
        assertThat(SearchKeywordUtils.buildContainsPattern(" SN_100\\A ")).isEqualTo("%SN!_100!\\A%");
    }

    @Test
    void containsConditionUsesBoundParameterAndEscapeClause() {
        assertThat(SearchKeywordUtils.containsCondition("part_code"))
                .isEqualTo("part_code LIKE {0} ESCAPE '!'");
    }
}
