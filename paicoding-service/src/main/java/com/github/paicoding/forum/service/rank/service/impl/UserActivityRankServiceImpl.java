package com.github.paicoding.forum.service.rank.service.impl;

import com.github.paicoding.forum.api.model.enums.rank.ActivityRankTimeEnum;
import com.github.paicoding.forum.api.model.vo.rank.dto.RankItemDTO;
import com.github.paicoding.forum.api.model.vo.user.dto.SimpleUserInfoDTO;
import com.github.paicoding.forum.core.cache.RedisClient;
import com.github.paicoding.forum.core.util.DateUtil;
import com.github.paicoding.forum.service.rank.service.UserActivityRankService;
import com.github.paicoding.forum.service.rank.service.model.ActivityScoreBo;
import com.github.paicoding.forum.service.user.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author YiHui
 * @date 2023/8/19
 */
@Slf4j
@Service
public class UserActivityRankServiceImpl implements UserActivityRankService {
    private static final String ACTIVITY_SCORE_KEY = "activity_rank_";

    @Autowired
    private UserService userService;

    /**
     * 当天活跃度排行榜
     *
     * @return 当天排行榜key
     */
    private String todayRankKey() {
        return ACTIVITY_SCORE_KEY + DateUtil.format(DateTimeFormatter.ofPattern("yyyyMMdd"), System.currentTimeMillis());
    }

    /**
     * 本月排行榜
     *
     * @return 月度排行榜key
     */
    private String monthRankKey() {
        return ACTIVITY_SCORE_KEY + DateUtil.format(DateTimeFormatter.ofPattern("yyyyMM"), System.currentTimeMillis());
    }

    /**
     * 添加活跃分
     *
     * @param userId
     * @param activityScore
     */
    @Override
    public void addActivityScore(Long userId, ActivityScoreBo activityScore) {
        if (userId == null) {
            return;
        }

        String field;
        int score = 0;
        if (activityScore.getPath() != null) {
            field = "path_" + activityScore.getPath();
            score = 1;
        } else if (activityScore.getArticleId() != null) {
            field = activityScore.getArticleId() + "_";
            if (activityScore.getPraise() != null) {
                field += "praise";
                score = BooleanUtils.isTrue(activityScore.getPraise()) ? 2 : -2;
            } else if (activityScore.getCollect() != null) {
                field += "collect";
                score = BooleanUtils.isTrue(activityScore.getCollect()) ? 2 : -2;
            } else if (activityScore.getRate() != null) {
                // 评论回复
                field += "rate";
                score = BooleanUtils.isTrue(activityScore.getRate()) ? 3 : -3;
            } else if (BooleanUtils.isTrue(activityScore.getPublishArticle())) {
                // 发布文章
                field += "publish";
                score += 10;
            }
        } else if (activityScore.getFollowedUserId() != null) {
            field = activityScore.getFollowedUserId() + "_follow";
            score = BooleanUtils.isTrue(activityScore.getFollow()) ? 2 : -2;
        } else {
            return;
        }

        final String todayRankKey = todayRankKey();
        final String monthRankKey = monthRankKey();
        final String userActionKey = ACTIVITY_SCORE_KEY + userId + DateUtil.format(DateTimeFormatter.ofPattern("yyyyMMdd"), System.currentTimeMillis());
        Integer ans = RedisClient.hGet(userActionKey, field, Integer.class);
        if (ans == null) {
            // 之前没有加分记录，执行具体的加分
            if (score > 0) {
                // 记录加分记录
                RedisClient.hSet(userActionKey, field, score);
                // 个人用户的操作记录，保存一个月的有效期，方便用户查询自己最近31天的活跃情况
                RedisClient.expire(userActionKey, 31 * DateUtil.ONE_DAY_SECONDS);

                // 更新当天和当月的活跃度排行榜
                Double newAns = RedisClient.zIncrBy(todayRankKey, String.valueOf(userId), score);
                log.info("新增评分! key#field = {}#{}, add = {}, newScore = {}", todayRankKey, userId, score, newAns);
                RedisClient.zIncrBy(monthRankKey, String.valueOf(userId), score);
                if (newAns <= score) {
                    // 日活跃榜单，保存31天；月活跃榜单，保存1年
                    RedisClient.expire(todayRankKey, 31 * DateUtil.ONE_DAY_SECONDS);
                    RedisClient.expire(monthRankKey, 12 * DateUtil.ONE_MONTH_SECONDS);
                }
            }
        } else if (ans > 0) {
            // 之前已经加过分，因此这次减分可以执行
            if (score < 0) {
                Boolean oldHave = RedisClient.hDel(userActionKey, field);
                if (BooleanUtils.isTrue(oldHave)) {
                    RedisClient.zIncrBy(todayRankKey, String.valueOf(userId), score);
                    RedisClient.zIncrBy(monthRankKey, String.valueOf(userId), score);
                }
            }
        }
    }

    @Override
    public RankItemDTO queryRankInfo(Long userId, ActivityRankTimeEnum time) {
        RankItemDTO item = new RankItemDTO();
        item.setUser(userService.querySimpleUserInfo(userId));

        String rankKey = time == ActivityRankTimeEnum.DAY ? todayRankKey() : monthRankKey();
        ImmutablePair<Integer, Double> rank = RedisClient.zRankInfo(rankKey, String.valueOf(userId));
        item.setRank(rank.getLeft());
        item.setScore(rank.getRight().intValue());
        return item;
    }

    @Override
    public List<RankItemDTO> queryRankList(ActivityRankTimeEnum time, int size) {
        String rankKey = time == ActivityRankTimeEnum.DAY ? todayRankKey() : monthRankKey();
        List<ImmutablePair<String, Double>> rankList = RedisClient.zTopNScore(rankKey, size);
        if (CollectionUtils.isEmpty(rankList)) {
            return Collections.emptyList();
        }

        Map<Long, Integer> userScoreMap = rankList.stream().collect(Collectors.toMap(s -> Long.valueOf(s.getLeft()), s -> s.getRight().intValue()));
        List<SimpleUserInfoDTO> users = userService.batchQuerySimpleUserInfo(userScoreMap.keySet());
        List<RankItemDTO> rank = new ArrayList<>();
        for (SimpleUserInfoDTO user : users) {
            RankItemDTO item = new RankItemDTO();
            item.setUser(user);
            item.setScore(userScoreMap.get(user.getUserId()));
            rank.add(item);
        }

        // 根据分数进行倒排
        rank.sort((o1, o2) -> Integer.compare(o2.getScore(), o1.getScore()));
        for (int i = 0, rankSize = rank.size(); i < rankSize; i++) {
            RankItemDTO s = rank.get(i);
            s.setRank(i + 1);
        }
        return rank;
    }
}