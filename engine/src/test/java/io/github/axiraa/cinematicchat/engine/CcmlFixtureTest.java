package io.github.axiraa.cinematicchat.engine;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

/** Neutral welcome fixtures covering timing, styles, literal fallback and limits. */
class CcmlFixtureTest {
    @Test void diagnosticsKeepCodesAndSourceOffsetsWithoutPresentationText(){
        var document=new CcmlParser().parse("Hello [pause=invalid]");
        assertEquals(new CcmlDiagnostic(CcmlDiagnostic.Code.LITERAL_MARKUP,6,""),document.diagnostics().getFirst());
        assertEquals("Hello [pause=invalid]",document.source().text());
        var limited=new CcmlParser().parse(io.github.axiraa.cinematicchat.api.StyledText.plain("a[br]b"),new CcmlLimits(100,1,16,3,10,.75,1.5,1,2,60),true);
        assertEquals(CcmlDiagnostic.Code.NODE_LIMIT,limited.diagnostics().getFirst().code());
    }
    record Fixture(String id,String script,String finalText) {
        @Override public String toString(){return "CCML fixture "+id;}
    }
    static Stream<Fixture> fixtures(){return Stream.of(
        new Fixture("A","您好，\\n[pause=1.2]欢迎使用Cinematic Chat。","您好，\n欢迎使用Cinematic Chat。"),
        new Fixture("B","[speed=15]您好，[wait][pause=0.5]\\n欢迎使用Cinematic Chat。","您好，\n欢迎使用Cinematic Chat。"),
        new Fixture("C","您好，[color=#B02020][shake]欢迎使用Cinematic Chat[/shake][/color]。","您好，欢迎使用Cinematic Chat。"),
        new Fixture("D","Hello, welcome to Cinematic Chat.[pause=1][replace]您好，欢迎使用Cinematic Chat。","您好，欢迎使用Cinematic Chat。"),
        new Fixture("E","你好[wait][erase=2]您好，欢迎使用Cinematic Chat。","您好，欢迎使用Cinematic Chat。"),
        new Fixture("F","这是\\[pause=5\\]普通文字。","这是[pause=5]普通文字。"),
        new Fixture("G","[pause=abc]","[pause=abc]"),
        new Fixture("H","[pause=999999999]",""));}

    @ParameterizedTest(name="{0}") @MethodSource("fixtures")
    void fixturesRunWithoutLosingLiteralText(Fixture fixture){
        var plan=new SubtitleTimeline().compile(new CcmlParser().parse(fixture.script()),EngineConfig.DEFAULT);
        var runtime=new SubtitleRuntime(1,plan);
        runtime.advance(1000,event->{});
        assertEquals(fixture.finalText(),runtime.snapshot().text());
        if(fixture.id().equals("H"))assertTrue(plan.completeAt()<=10);
        if(fixture.id().equals("F"))assertFalse(plan.actions().stream().anyMatch(a->a.event()==SubtitleRuntimeEvent.Kind.PAUSE_STARTED));
        if(fixture.id().equals("C")){
            var glyph=runtime.snapshot().glyphs().get(3).glyph();
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
