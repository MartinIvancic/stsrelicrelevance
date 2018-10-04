package relicrelevance;

import static org.reflections.ReflectionUtils.getAllMethods;
import static org.reflections.ReflectionUtils.withName;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.reflections.Reflections;

import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;
import com.megacrit.cardcrawl.relics.AbstractRelic;


@SpireInitializer
public class RelicType {

	public static final Map<String, Types> RELICS_METADATA = new TreeMap<>();

	private static final Logger logger = LogManager.getLogger(RelicType.class.getName());
	private static final String RELIC_PACKAGE = "com.megacrit.cardcrawl.relics";
	
	// Relics that need type override
	private static final String[] HERO_BASE_RELICS = { "BurningBlood", "BlackBlood", "CrackedCore", "FrozenCore",
			"SnakeRing", "RingOfTheSerpent" };
	private static final String[] ON_ATTACK_MONSTER_RELICS = {"SneckoSkull", "HandDrill", "ChemicalX", "ChampionsBelt", "BlueCandle"};	
	private static final String[] ON_EXHAUST_RELICS = {"StrangeSpoon", "CharonsAshes", "DeadBranch"};	
	private static final String[] AT_TURN_START_RELICS = { "BustedCrown", "IceCream", "PaperFrog", "PaperCrane", "OddMushroom", "WristBlade", 
			"Turnip", "MedicalKit", "Calipers", "Sozu", "RunicDome", "CoffeeDripper", "Ectoplasm", "FusionHammer", "Ginger", "HoveringKite", "MagicFlower",
			"WarpedTongs"};
	private static final String[] ON_LOSE_HP_RELICS = {"CentennialPuzzle", "RunicCube", "GoldPlatedCables"};
	private static final String[] ON_PLAYER_END_TURN_RELICS = {"RunicPyramid"};
	private static final String[] BATTLE_START_PRE_DRAW_RELICS = {"BagOfPreparation"};
	private static final String[] ON_VICTORY_RELICS = {"WhiteBeast", "SingingBowl", "QuestionCard", "PrayerWheel", "NlothsGift", "BloodyIdol", "PrismaticShard"};
	private static final String[] ON_ENTER_REST_ROOM_RELICS = {"RegalPillow", "AncientTeaSet", "Shovel", "RegalPillow", "PeacePipe", "DreamCatcher"};
	private static final String[] SET_COUNTER_RELICS = {"NlothsMask", "Omamori"};
	private static final String[] ON_ENTER_ROOM_RELICS = {"JuzuBracelet", "TinyChest"};
	private static final String[] ON_EQUIP_RELICS = {"CallingBell", "Waffle", "WarPaint", "Whetstone", "TinyHouse", "Strawberry", 
			"SpiritPoop", "PotionBelt", "Pear", "Orrery", "PandorasBox", "OldCoin", "DollysMirror", "EmptyCage", "FrozenEye", "GoldenIdol", "Mango", "MarkOfTheBloom", "MeatOnTheBone"};

	enum Types {
		STARTING(-200, "none", HERO_BASE_RELICS),
		USE_CARD(-100, "onUseCard", null),
		ON_ATTACK_MONSTER(-90, "onAttackedMonster", ON_ATTACK_MONSTER_RELICS), 
		ON_MANUAL_DISCARD(-86, "onManualDiscard", null), 
		ON_EXHAUST(-85, "onExhaust", ON_EXHAUST_RELICS), 
		ON_ATTACKED(-75, "onAttacked", null),
		ON_LOSE_HP(-73, "onLoseHp", ON_LOSE_HP_RELICS), 
		ON_MONSTER_DEATH(-70, "onMonsterDeath", null), 
		AT_TURN_START(-68, "atTurnStart", AT_TURN_START_RELICS),
		ON_TRIGGER(-67, "onTrigger", null), 
		ON_USE_POTION(-65, "onUsePotion", null), 
		ON_PLAYER_END_TURN(-64, "onPlayerEndTurn", ON_PLAYER_END_TURN_RELICS),
		ON_SHUFFLE(-63, "onShuffle", null), 
		AT_BATTLE_START(-60, "atBattleStart", null), 
		BATTLE_START_PRE_DRAW(-50, "atBattleStartPreDraw", BATTLE_START_PRE_DRAW_RELICS), 
		AT_PRE_BATTLE(-45, "atPreBattle", null), 
		ON_VICTORY(-42, "onVictory", ON_VICTORY_RELICS),
		ON_PLAYER_HEAL(-41, "onPlayerHeal", null),
		ON_OBTAIN_CARD(-40, "onObtainCard", null),
		ON_ENTER_ROOM(-30, "onEnterRoom", ON_ENTER_REST_ROOM_RELICS),
		ON_ENTER_REST_ROOM(-27, "onEnterRestRoom", ON_ENTER_ROOM_RELICS),
		ON_CHEST_OPEN(-25, "onChestOpen", null), 
		SET_COUNTER(-20, "setCounter", SET_COUNTER_RELICS), 
		ON_EQUIP(-5, "onEquip", ON_EQUIP_RELICS), 
		DEFAULT(1, "none", null),
		DEPLETED(100, "none", null);

		private int ordering;
		private final String methodName;
		private final String[] overrides;

		private Types(int order, String methodName, String[] overrides) {
			this.ordering = order;
			this.methodName = methodName;
			this.overrides = overrides;
		}

		public int getOrdering() {
			return this.ordering;
		}

		public String getMethodName() {
			return methodName;
		}

		public String[] getOverrides() {
			return overrides;
		}
	}
	
	public static void main(String... ags) {
		resetRelicMetadata();
	}

	// no need to ever instantiate this class
	private RelicType() {
	}

	public static void initialize() {		
		resetRelicMetadata();
	}
	
	private static void resetRelicMetadata() {
		inferRelicType();

		if (logger.isDebugEnabled()) {
			logger.debug("Sorted relic type list:");
			int count = 1;
			for (String relic : RELICS_METADATA.keySet()) {
				logger.debug("{}: {} - {}", count++, relic, RELICS_METADATA.get(relic));
			}
		}
	}

	private static void inferRelicType() {
		logger.info("Determining type for each relic in {}", RELIC_PACKAGE);

		Reflections reflections = new Reflections(RELIC_PACKAGE);

		Set<Class<? extends AbstractRelic>> relicsClasses = reflections.getSubTypesOf(AbstractRelic.class);

		relicLoop:
		for (Class<? extends AbstractRelic> relicClass : relicsClasses) {

			// add all as default first
			RELICS_METADATA.put(relicClass.getSimpleName(), Types.DEFAULT);

			if (isStartingOrUpgraded(relicClass)) {
				continue;
			}
			
			for(Types type : Types.values()) {
				if (categorizeRelicsOf(relicClass, type)) {
					continue relicLoop;
				}
			}
		}
		
		// apply overrides
		overrideLoop:
		for (Class<? extends AbstractRelic> relicClass : relicsClasses) {
			for(Types type : Types.values()) {
				String[] overridesForType = type.getOverrides();
				
				if(overridesForType != null) {
					for(String override : overridesForType) {
						if(override.equalsIgnoreCase(relicClass.getSimpleName())) {
							RELICS_METADATA.put(relicClass.getSimpleName(), type);
							continue overrideLoop;
						}
					}
				}
			}
		}

		logger.info("Found and determined the type for {} relics", RELICS_METADATA.size());
	}

	private static boolean isStartingOrUpgraded(Class<? extends AbstractRelic> relicClass) {

		for (int i = 0; i < HERO_BASE_RELICS.length; i++) {
			if (HERO_BASE_RELICS[i].equalsIgnoreCase(relicClass.getSimpleName())) {
				RELICS_METADATA.put(relicClass.getSimpleName(), Types.STARTING);
				return true;
			}
		}

		return false;
	}
	
	private static boolean categorizeRelicsOf(Class<? extends AbstractRelic> relicClass, RelicType.Types type) {
		
		if(overrideRelicsOf(relicClass, type)) {
			return true;
		}

		/*
		 * The varargs array is not type safe by Java standards, but since it's
		 * "hardcoded" we're fine here 
		 */
		@SuppressWarnings("unchecked")
		Set<Method> methods = getAllMethods(relicClass, withName(type.getMethodName()));

		return addForType(relicClass, methods, type);
	}
	
	private static boolean overrideRelicsOf(Class<? extends AbstractRelic> relicClass, Types type) {
		
		if(type.getOverrides() == null) {
			return false;
		}
		
		if(Arrays.asList(type.getOverrides()).contains(relicClass.getSimpleName())) {
			RELICS_METADATA.put(relicClass.getSimpleName(), type);
			return true;
		}

		return false;
	}	

	private static boolean addForType(Class<? extends AbstractRelic> relicClass, Set<Method> methods, Types relicType) {
		if (methods.size() > 0) {
			for (Method method : methods) {
				// skip empty methods inherited from AbstractRelic class
				if (method.toGenericString().contains(AbstractRelic.class.getSimpleName())) {
					continue;
				}

				RELICS_METADATA.put(relicClass.getSimpleName(), relicType);
				return true;
			}
		}

		return false;
	}

}
