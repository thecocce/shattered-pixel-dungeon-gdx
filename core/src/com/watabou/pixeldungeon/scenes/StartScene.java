/*
 * Copyright (C) 2012-2014  Oleg Dolya
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */
package com.watabou.pixeldungeon.scenes;

import java.util.HashMap;

import com.watabou.input.NoosaInputProcessor;
import com.watabou.noosa.BitmapText;
import com.watabou.noosa.BitmapTextMultiline;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.noosa.NinePatch;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.TouchArea;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.ui.Button;
import com.watabou.pixeldungeon.Assets;
import com.watabou.pixeldungeon.Badges;
import com.watabou.pixeldungeon.Chrome;
import com.watabou.pixeldungeon.Dungeon;
import com.watabou.pixeldungeon.GamesInProgress;
import com.watabou.pixeldungeon.PixelDungeon;
import com.watabou.pixeldungeon.actors.hero.HeroClass;
import com.watabou.pixeldungeon.actors.hero.HeroSubClass;
import com.watabou.pixeldungeon.effects.Speck;
import com.watabou.pixeldungeon.input.GameAction;
import com.watabou.pixeldungeon.ui.Archs;
import com.watabou.pixeldungeon.ui.Icons;
import com.watabou.pixeldungeon.ui.RedButton;
import com.watabou.pixeldungeon.ui.SimpleButton;
import com.watabou.pixeldungeon.ui.Window;
import com.watabou.pixeldungeon.utils.Utils;
import com.watabou.pixeldungeon.windows.WndList;
import com.watabou.pixeldungeon.windows.WndOptions;
import com.watabou.pixeldungeon.windows.WndTitledMessage;

public class StartScene extends PixelScene {

	private static final float BUTTON_HEIGHT	= 24;
	private static final float GAP				= 2;
	
	private static final String TXT_TITLE	= "Select Your Hero";
	
	private static final String TXT_LOAD	= "Load Game";
	private static final String TXT_NEW		= "New Game";
	
	private static final String TXT_ERASE		= "Erase current game";
	private static final String TXT_DPTH_LVL	= "Depth: %d, level: %d";
	
	private static final String TXT_REALLY	= "Do you really want to start new game?";
	private static final String TXT_WARNING	= "Your current game progress will be erased.";
	private static final String TXT_YES		= "Yes, start new game";
	private static final String TXT_NO		= "No, return to main menu";
	
	private static final String TXT_UNLOCK	= "To unlock this character class, slay the 3rd boss with any other class";
	
	private static final String TXT_WIN_THE_GAME = 
		"To unlock \"Challenges\", win the game with any character class.";
	
	private static final float WIDTH = 116;
	private static final float HEIGHT = 220;
	
	private static HashMap<HeroClass, ClassShield> shields = new HashMap<HeroClass, ClassShield>();
	
	private GameButton btnLoad;
	private GameButton btnNewGame;
	
	private boolean huntressUnlocked;
	private Group unlock;
	
	public static HeroClass curClass;
	
	@Override
	public void create() {
		
		super.create();
		
		Badges.loadGlobal();
		
		uiCamera.visible = false;
		
		int w = Camera.main.width;
		int h = Camera.main.height;

		float left = (w - WIDTH) / 2;
		float top = (h - HEIGHT) / 2; 
		float bottom = h - top;
		
		Archs archs = new Archs();
		archs.setSize( w, h );
		add( archs ); 
		
		Image title = BannerSprites.get( Type.SELECT_YOUR_HERO );
		title.x = align( (w - title.width()) / 2 );
		title.y = top;
		add( title );
		
		btnNewGame = new GameButton( TXT_NEW ) {
			@Override
			protected void onClick() {
				if (GamesInProgress.check( curClass ) != null) {
					StartScene.this.add( new WndOptions( TXT_REALLY, TXT_WARNING, TXT_YES, TXT_NO ) {
						@Override
						protected void onSelect( int index ) {
							if (index == 0) {
								startNewGame();
							}
						}
					} );
					
				} else {
					startNewGame();
				}
			}
		};
		add( btnNewGame );

		btnLoad = new GameButton( TXT_LOAD ) {	
			@Override
			protected void onClick() {
				InterlevelScene.mode = InterlevelScene.Mode.CONTINUE;
				Game.switchScene( InterlevelScene.class );
			}
		};
		add( btnLoad );
		
		frame.size( width, BUTTON_HEIGHT + frame.marginVer() );
		frame.x = align( left );
		frame.y = align( h - top - frame.height() );
		
		avatar = new Avatar();
		
		NinePatch avFrame = Chrome.get( Chrome.Type.TOAST_TR );
		avFrame.size( avatar.width() * 1.6f, avatar.height() * 1.6f );
		avFrame.x = align( (w - avFrame.width()) / 2 );
		avFrame.y = align( (frame.y + btns[0].bottom() - avFrame.height()) / 2 );
		add( avFrame );
		
		className = PixelScene.createText( "Placeholder", 9 );
		className.measure();
		className.y = align( avFrame.y + avFrame.innerBottom() - className.height() );
		add( className );
		
		avatar.point( avFrame.center() );
		avatar.camera = Camera.main;
		align( avatar );
		add( avatar );
		
		Image iconInfo = Icons.INFO.get();
		iconInfo.x = avFrame.x + avFrame.innerRight() - iconInfo.width();
		iconInfo.y = avFrame.y + avFrame.marginTop();
		add( iconInfo );
		
		add( new TouchArea( avFrame ) {
			@Override
			protected void onClick( NoosaInputProcessor.Touch touch ) {
				add( new WndList( curClass.perks() ) );
			}
		} );
		
		btnMastery = new SimpleButton( Icons.MASTERY.get() ) {
			@Override
			protected void onClick() {
				String text = null;
				switch (curClass) {
				case WARRIOR:
					text = HeroSubClass.GLADIATOR.desc() + "\n\n" + HeroSubClass.BERSERKER.desc();
					break;
				case MAGE:
					text = HeroSubClass.BATTLEMAGE.desc() + "\n\n" + HeroSubClass.WARLOCK.desc();
					break;
				case ROGUE:
					text = HeroSubClass.FREERUNNER.desc() + "\n\n" + HeroSubClass.ASSASSIN.desc();
					break;
				case HUNTRESS:
					text = HeroSubClass.SNIPER.desc() + "\n\n" + HeroSubClass.WARDEN.desc();
					break;
				}
				StartScene.this.add( new WndTitledMessage( Icons.MASTERY.get(), "Mastery", text ) );
			}
		};
		btnMastery.setPos( 
			avFrame.x + avFrame.innerRight() - btnMastery.width(),
			avFrame.y + avFrame.innerBottom() - btnMastery.height() );
		add( btnMastery );
		
		unlock = new Group();
		add( unlock );
		
		if (!(huntressUnlocked = Badges.isUnlocked( Badges.Badge.BOSS_SLAIN_3 ))) {
		
			BitmapTextMultiline text = PixelScene.createMultiline( TXT_UNLOCK, 5 );
			text.maxWidth = (int)frame.innerWidth();
			text.measure();
			
			pos = frame.center().y - text.height() / 2;
			for (BitmapText line : text.new LineSplitter().split()) {
				line.measure();
				line.hardlight( 0xFFFF00 );
				line.x = PixelScene.align( frame.center().x - line.width() / 2 );
				line.y = PixelScene.align( pos );
				unlock.add( line );
				
				pos += line.height(); 
			}
		}
		
		ExitButton btnExit = new ExitButton();
		btnExit.setPos( Camera.main.width - btnExit.width(), 0 );
		add( btnExit );
		
		curClass = null;
		updateClass( HeroClass.values()[PixelDungeon.lastClass()] );
		
		fadeIn();
	}
	
	private void updateClass( HeroClass cl ) {
		
		if (curClass == cl) {
			add( new WndClass( cl ) );
			return;
		}
		
		if (curClass != null) {
			shields.get( curClass ).highlight( false );
		}
		shields.get( curClass = cl ).highlight( true );
		
		if (cl != HeroClass.HUNTRESS || huntressUnlocked) {
		
			unlock.visible = false;
			 
			float buttonPos = (Camera.main.height + HEIGHT) / 2 - BUTTON_HEIGHT;
			
			float left = (Camera.main.width - WIDTH) / 2;
			
			GamesInProgress.Info info = GamesInProgress.check( curClass );
			if (info != null) {
				
				btnLoad.visible = true;
				btnLoad.secondary( Utils.format( TXT_DPTH_LVL, info.depth, info.level ) );
				btnNewGame.visible = true;
				btnNewGame.secondary( TXT_ERASE );
				
				float w = (WIDTH - GAP) / 2;
				
				btnLoad.setRect(
					left, buttonPos, w, BUTTON_HEIGHT );
				btnNewGame.setRect(
					btnLoad.right() + GAP, buttonPos, w, BUTTON_HEIGHT );
				
			} else {
				btnLoad.visible = false;
				
				btnNewGame.visible = true;
				btnNewGame.secondary( null );
				btnNewGame.setRect( left, buttonPos, WIDTH, BUTTON_HEIGHT );
			}
			
		} else {
			
			unlock.visible = true;
			btnLoad.visible = false;
			btnNewGame.visible = false;
			
		}
	}
	
	private void startNewGame() {

		Dungeon.hero = null;
		InterlevelScene.mode = InterlevelScene.Mode.DESCEND;
		
		if (PixelDungeon.intro()) {
			PixelDungeon.intro( false );
			Game.switchScene( IntroScene.class );
		} else {
			Game.switchScene( InterlevelScene.class );
		}	
	}
	
	@Override
	protected void onBackPressed() {
		Game.switchScene( TitleScene.class );
	}
	
	private static class Avatar extends Image {
		
		private static final int WIDTH	= 24;
		private static final int HEIGHT	= 32;
		private static final int SCALE	= 2;
		
		private TextureFilm frames;
		
		private float brightness = 0;
		
		public Avatar() {
			super( Assets.AVATARS );
			
			frames = new TextureFilm( texture, WIDTH, HEIGHT );
			selectClass( HeroClass.WARRIOR );
			scale.set( SCALE );
			
			origin.set( width() / 2, height() / 2 );
		}
		
		public void selectClass( HeroClass cl ) {
			frame( frames.get( cl.ordinal() ) );
		}
		
		public void flash() {
			brightness = 1f;
		}
		
		@Override
		public void update() {
			super.update();
		
			if (brightness > 0) {
				ra = ga = ba = brightness;
				brightness -= Game.elapsed * 4;
				if (brightness < 0) {
					resetColor();
				}
			}
		}
	}
	
	private class GemButton extends Button<GameAction> {
		
		private NinePatch bg;
		private Image icon;
		
		private HeroClass cl;
		
		public GemButton( HeroClass cl ) {
			super();
			
			this.cl = cl;
			gems.put( cl, this );
			
			icon.copy( Icons.get( cl ) );
			setSize( 32, 32 );
			
			highlight( false );
		}
		
		@Override
		protected void createChildren() {
			super.createChildren();
			
			bg = Chrome.get( Chrome.Type.GEM );
			add( bg );
			
			icon = new Image();
			add( icon );
		}
		
		@Override
		protected void layout() {
			super.layout();
			
			bg.x = x;
			bg.y = y;
			bg.size( width, height );
			
			icon.x = x + (width - icon.width) / 2;
			icon.y = y + (height - icon.height) / 2;
		}
		
		@Override
		protected void onTouchDown() {
			Emitter emitter = (Emitter)recycle( Emitter.class );
			emitter.revive();
			emitter.pos( bg );
			emitter.burst( Speck.factory( Speck.LIGHT ), 3 );
			
			updateClass( cl );
			avatar.flash();
			
			Sample.INSTANCE.play( Assets.SND_CLICK, 1, 1, 1.2f );
		}
		
		public void highlight( boolean value ) {
			if (value) {
				bg.rm = 1.2f;
				bg.gm = 1.2f;
				bg.bm = 1.1f;
				bg.am = 0.8f;
			} else {
				bg.rm = 1.0f;
				bg.gm = 1.0f;
				bg.bm = 1.0f;
				bg.am = 0.6f;
			}
		}
	}
	
	private static class GameButton extends RedButton {
		
		private static final int SECONDARY_COLOR	= 0xCACFC2;
		
		private BitmapText secondary;
		
		public GameButton( String primary ) {
			super( primary );
			
			this.secondary.text( null );
		}
		
		@Override
		protected void createChildren() {
			super.createChildren();
			
			secondary = createText( 6 );
			secondary.hardlight( SECONDARY_COLOR );
			add( secondary );
		}
		
		@Override
		protected void layout() {
			super.layout();
			
			if (secondary.text().length() > 0) {
				text.y = y + (height - text.height() - secondary.baseLine()) / 2;
				
				secondary.x = align( x + (width - secondary.width()) / 2 );
				secondary.y = align( text.y + text.height() ); 
			} else {
				text.y = y + (height - text.baseLine()) / 2;
			}
		}
		
		public void secondary( String text ) {
			secondary.text( text );
			secondary.measure();
		}
	}
}
