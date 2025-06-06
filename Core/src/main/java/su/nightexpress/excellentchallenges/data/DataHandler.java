package su.nightexpress.excellentchallenges.data;

import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import su.nightexpress.excellentchallenges.challenge.GeneratedChallenge;
import su.nightexpress.excellentchallenges.data.object.ChallengeUser;
import su.nightexpress.excellentchallenges.data.serialize.GenChallengeSerializer;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.excellentchallenges.ChallengesPlugin;
import su.nightexpress.excellentchallenges.data.serialize.UniIntSerializer;
import su.nightexpress.nightcore.database.AbstractUserDataHandler;
import su.nightexpress.nightcore.database.sql.SQLColumn;
import su.nightexpress.nightcore.database.sql.SQLValue;
import su.nightexpress.nightcore.database.sql.column.ColumnType;
import su.nightexpress.nightcore.util.wrapper.UniInt;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Function;

public class DataHandler extends AbstractUserDataHandler<ChallengesPlugin, ChallengeUser> {

    private static final SQLColumn COL_CHALLENGES           = SQLColumn.of("challenges", ColumnType.STRING);
    private static final SQLColumn COL_REFRESH_TIMES        = SQLColumn.of("refreshTimes", ColumnType.STRING);
    private static final SQLColumn COL_REROLL_TOKENS        = SQLColumn.of("rerollTokens", ColumnType.STRING);
    private static final SQLColumn COL_COMPLETED_CHALLENGES = SQLColumn.of("completedChallenges", ColumnType.STRING);
    private static final SQLColumn COL_COMPLETED_MILESTONES = SQLColumn.of("completedMilestones", ColumnType.STRING);

    private final  Function<ResultSet, ChallengeUser> userFunction;

    public DataHandler(@NotNull ChallengesPlugin plugin) {
        super(plugin);

        this.userFunction = (resultSet) -> {
            try {
                UUID uuid = UUID.fromString(resultSet.getString(COLUMN_USER_ID.getName()));
                String name = resultSet.getString(COLUMN_USER_NAME.getName());
                long lastOnline = resultSet.getLong(COLUMN_USER_LAST_ONLINE.getName());
                long dateCreated = resultSet.getLong(COLUMN_USER_DATE_CREATED.getName());

                Map<String, Set<GeneratedChallenge>> challenges = gson.fromJson(resultSet.getString(COL_CHALLENGES.getName()), new TypeToken<Map<String, Set<GeneratedChallenge>>>(){}.getType());
                challenges.values().removeIf(Objects::isNull);

                Map<String, Long> refreshTimes = gson.fromJson(resultSet.getString(COL_REFRESH_TIMES.getName()), new TypeToken<Map<String, Long>>(){}.getType());

                Map<String, Integer> rerollTokens = gson.fromJson(resultSet.getString(COL_REROLL_TOKENS.getName()), new TypeToken<Map<String, Integer>>(){}.getType());

                Map<String, Map<String, Integer>> completedChallenges = gson.fromJson(resultSet.getString(COL_COMPLETED_CHALLENGES.getName()), new TypeToken<Map<String, Map<String, Integer>>>(){}.getType());

                Map<String, Set<Integer>> completedMilestones = gson.fromJson(resultSet.getString(COL_COMPLETED_MILESTONES.getName()), new TypeToken<Map<String, Set<Integer>>>(){}.getType());
                if (completedMilestones == null) completedMilestones = new HashMap<>();

                return new ChallengeUser(plugin, uuid, name, dateCreated, lastOnline,
                    challenges, refreshTimes, rerollTokens, completedChallenges, completedMilestones);
            }
            catch (SQLException exception) {
                exception.printStackTrace();
                return null;
            }
        };
    }

    @Override
    protected void createUserTable() {
        super.createUserTable();

        this.addColumn(this.tableUsers, COL_COMPLETED_MILESTONES.toValue("[]"));
    }

    @Override
    public void onSynchronize() {
        for (ChallengeUser user : this.plugin.getUserManager().getLoaded()) {
            if (this.plugin.getUserManager().isScheduledToSave(user)) continue;

            ChallengeUser fresh = this.getUser(user.getId());
            if (fresh == null) continue;

            if (!user.isSyncReady()) continue;

            user.getChallengesMap().clear();
            user.getChallengesMap().putAll(fresh.getChallengesMap());

            user.getRefreshTimes().clear();
            user.getRefreshTimes().putAll(fresh.getRefreshTimes());

            user.getRerollTokens().clear();
            user.getRerollTokens().putAll(fresh.getRerollTokens());

            user.getCompletedChallengesMap().clear();
            user.getCompletedChallengesMap().putAll(fresh.getCompletedChallengesMap());

            user.getCompletedMilestonesMap().clear();
            user.getCompletedMilestonesMap().putAll(fresh.getCompletedMilestonesMap());
        }
    }

    @Override
    @NotNull
    protected GsonBuilder registerAdapters(@NotNull GsonBuilder builder) {
        return super.registerAdapters(builder)
            .registerTypeAdapter(UniInt.class, new UniIntSerializer())
            .registerTypeAdapter(GeneratedChallenge.class, new GenChallengeSerializer(this.plugin));
    }

    @Override
    @NotNull
    protected List<SQLColumn> getExtraColumns() {
        return Arrays.asList(COL_CHALLENGES, COL_REFRESH_TIMES, COL_REROLL_TOKENS, COL_COMPLETED_CHALLENGES, COL_COMPLETED_MILESTONES);
    }

    @Override
    @NotNull
    protected List<SQLValue> getSaveColumns(@NotNull ChallengeUser user) {
        return Arrays.asList(
            COL_CHALLENGES.toValue(this.gson.toJson(user.getChallengesMap())),
            COL_REFRESH_TIMES.toValue(this.gson.toJson(user.getRefreshTimes())),
            COL_REROLL_TOKENS.toValue(this.gson.toJson(user.getRerollTokens())),
            COL_COMPLETED_CHALLENGES.toValue(this.gson.toJson(user.getCompletedChallengesMap())),
            COL_COMPLETED_MILESTONES.toValue(this.gson.toJson(user.getCompletedMilestonesMap()))
        );
    }

    @Override
    @NotNull
    protected Function<ResultSet, ChallengeUser> getUserFunction() {
        return this.userFunction;
    }
}
