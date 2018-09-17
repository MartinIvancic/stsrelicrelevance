package relicrelevance;

import java.util.Comparator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.rooms.AbstractRoom;

import basemod.BaseMod;
import basemod.interfaces.OnStartBattleSubscriber;
import basemod.interfaces.PostInitializeSubscriber;
import basemod.interfaces.RelicGetSubscriber;
import relicrelevance.RelicType;

@SpireInitializer
public class RelicRelevance implements OnStartBattleSubscriber, PostInitializeSubscriber, RelicGetSubscriber  {

	private static final Logger logger = LogManager.getLogger(RelicRelevance.class.getName());

	private static final String NAME = "Relic Relevance";
	private static final String AUTHOR = "MartinI";
	private static final String DESCRIPTION = "Orders relics from most relevant (e.g. countdown relics) to irrelevant (e.g. all charges used).";
	private static final String BADGE = "Badge.png";
	
	public RelicRelevance() {
		BaseMod.subscribe(this);
	}

	public static void initialize() {
		new RelicRelevance();
	}

	@Override
	public void receiveOnBattleStart(AbstractRoom room) {
		logger.debug("RelicRelevance called on battle start");

		sortRelicsByRelevance();
	}
	
	@Override
	public void receiveRelicGet(AbstractRelic arg0) {
		logger.debug("RelicRelevance called on relic get");

		sortRelicsByRelevance();
	}

	@Override
	public void receivePostInitialize() {		
		BaseMod.registerModBadge(new Texture(Gdx.files.internal(BADGE)), NAME, AUTHOR, DESCRIPTION, null);
	}

	private void sortRelicsByRelevance() {		
		final int arraySize = AbstractDungeon.player.relics.size();
		float[][] locations = new float[arraySize][2]; 
		
		logger.debug("Relic list as returned by AbstractDungeon.player.relics has {} elements", arraySize) ;
		
		// save relic positions on screen
		for (int i = 0; i < arraySize; i++) {
			AbstractRelic relic = AbstractDungeon.player.relics.get(i);

			locations[i][0] = relic.currentX;
			locations[i][1] = relic.hb.x;
		}
		
		// sort the relics by relevance
		AbstractDungeon.player.relics.sort(new RelevanceComparator());
		
		logger.debug("Sorted relic list:");
		if(logger.isDebugEnabled()) {
			int count = 1;
			for(AbstractRelic relic : AbstractDungeon.player.relics) {
				logger.debug(count++ + " - " + relic.getClass().getSimpleName());
			}
		}
		
		// set the sorted relics' location on screen
		for (int i = 0; i < arraySize; i++) {
			AbstractRelic relic = AbstractDungeon.player.relics.get(i);
			
			relic.currentX = locations[i][0];
			relic.hb.x = locations[i][1];
		}
	}

	public static class RelevanceComparator implements Comparator<AbstractRelic> {

		@Override
		public int compare(AbstractRelic firstRelic, AbstractRelic secondRelic) {
			if (firstRelic == null || secondRelic == null) {
				return 0;
			}
			
			if(RelicType.RELICS_METADATA != null && RelicType.RELICS_METADATA.size() > 0) {
				
				RelicType.Types firstRelicType = RelicType.RELICS_METADATA.get(firstRelic.getClass().getSimpleName());
				RelicType.Types secondRelicType = RelicType.RELICS_METADATA.get(secondRelic.getClass().getSimpleName());
				
				if(firstRelicType == null || secondRelicType == null) {
					return 0;
				}

				if(firstRelicType != null && secondRelicType != null) {
					return  firstRelicType.getOrdering() - secondRelicType.getOrdering();
				}
			}

			return 0;
		}
	}
}
