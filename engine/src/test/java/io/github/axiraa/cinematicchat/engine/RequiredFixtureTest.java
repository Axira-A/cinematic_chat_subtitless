package io.github.axiraa.cinematicchat.engine;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

/** Exact A–H inputs from requirement 34, in addition to the semantic tests. */
class RequiredFixtureTest {
    record Fixture(String id,String script,String finalText) {
        @Override public String toString(){return "Required fixture "+id;}
    }
    static Stream<Fixture> fixtures(){return Stream.of(
        new Fixture("A","你终于来了。\\n[pause=1.2]不过……已经太迟了。","你终于来了。\n不过……已经太迟了。"),
        new Fixture("B","[speed=15]潮水正在上涨……[wait][pause=0.5]\\n快走。","潮水正在上涨……\n快走。"),
        new Fixture("C","不要碰那个[color=#B02020][shake]东西[/shake][/color]。","不要碰那个东西。"),
        new Fixture("D","欢迎回来，勇者。[pause=1][replace]欢迎回来，罪人。","欢迎回来，罪人。"),
        new Fixture("E","勇者[wait][erase=2]罪人。","罪人。"),
        new Fixture("F","这是\\[pause=5\\]普通文字。","这是[pause=5]普通文字。"),
        new Fixture("G","[pause=abc]","[pause=abc]"),
        new Fixture("H","[pause=999999999]",""));}

    @ParameterizedTest(name="{0}") @MethodSource("fixtures")
    void originalRequestFixturesRunWithoutLosingLiteralText(Fixture fixture){
        var plan=new SubtitleTimeline().compile(new CcmlParser().parse(fixture.script()),EngineConfig.DEFAULT);
        var runtime=new SubtitleRuntime(1,plan);
        runtime.advance(1000,event->{});
        assertEquals(fixture.finalText(),runtime.snapshot().text());
        if(fixture.id().equals("H"))assertTrue(plan.completeAt()<=10);
        if(fixture.id().equals("F"))assertFalse(plan.actions().stream().anyMatch(a->a.event()==SubtitleRuntimeEvent.Kind.PAUSE_STARTED));
        if(fixture.id().equals("C")){
            var glyph=runtime.snapshot().glyphs().get(5).glyph();
            assertEquals(0xB02020,glyph.style().color());assertTrue(glyph.style().shake()>0);
        }
    }
    @Test void fadeOverridesAreFiniteClampedAndWaitUsesTheirDuration(){
        var parser=new CcmlParser();var timeline=new SubtitleTimeline();
        var plan=timeline.compile(parser.parse("[fadein=9][fadeout=-1][reveal=instant]a[wait]b"),EngineConfig.DEFAULT);
        assertEquals(3,plan.entryDuration());assertEquals(0,plan.exitDuration());assertTrue(plan.completeAt()>=3);
        var zero=timeline.compile(parser.parse("[fadein=0][fadeout=3]a"),EngineConfig.DEFAULT);
        assertEquals(0,zero.entryDuration());assertEquals(3,zero.exitDuration());
    }
}
