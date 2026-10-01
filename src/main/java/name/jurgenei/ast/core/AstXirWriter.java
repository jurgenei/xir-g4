package name.jurgenei.ast.core;

import name.jurgenei.xir.XirSerializer;
import name.jurgenei.ast.core.model.AstClass;
import name.jurgenei.ast.core.model.AstInheritance;
import name.jurgenei.ast.core.model.AstModel;
import name.jurgenei.ast.core.model.AstRelation;
import name.jurgenei.ast.core.model.RelationKind;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import java.io.StringWriter;
import java.util.Comparator;

/**
 * Renders AST class model as canonical S-expression text.
 */
public final class AstXirWriter {
    private static final String INTERNAL_XDM_URI = "urn:name.jurgenei.xml:xdm";
    private static final String INTERNAL_XDM_PREFIX = "xdm";

    public String write(final AstModel astModel) {
        final StringWriter out = new StringWriter();
        final XirSerializer serializer = new XirSerializer(
                out,
                XirSerializer.OutputFormat.COMPACT,
                XirSerializer.SyntaxMode.LEGACY
        );

        serializer.startDocument();

        astModel.classes().stream()
                .map(AstClass::name)
                .sorted()
                .forEach(name -> emit(serializer, "class", name));

        astModel.relations().stream()
                .sorted(Comparator
                        .comparing(AstRelation::sourceClass)
                        .thenComparing(AstRelation::roleName)
                        .thenComparing(AstRelation::targetClass))
                .forEach(rel -> emit(
                        serializer,
                        rel.kind() == RelationKind.REF ? "ref" : "rel",
                        rel.sourceClass(),
                        rel.roleName(),
                        rel.targetClass(),
                        rel.cardinality().symbol()
                ));

        astModel.inheritances().stream()
                .sorted(Comparator
                        .comparing(AstInheritance::childClass)
                        .thenComparing(AstInheritance::parentClass))
                .forEach(isa -> emit(serializer, "isa", isa.childClass(), isa.parentClass()));

        try {
            serializer.endDocument();
        } catch (SAXException exception) {
            throw new IllegalStateException("Failed to render AST model as S-expression", exception);
        }
        return out.toString();
    }

    private static void emit(final XirSerializer serializer, final String nodeName, final String... values) {
        serializer.startElement("", nodeName, nodeName, new AttributesImpl());
        for (String value : values) {
            final AttributesImpl literalAttributes = new AttributesImpl();
            literalAttributes.addAttribute("", "value", "value", "CDATA", value);
            literalAttributes.addAttribute("", "quoted", "quoted", "CDATA", "false");
            serializer.startElement(
                    INTERNAL_XDM_URI,
                    "literal",
                    INTERNAL_XDM_PREFIX + ":literal",
                    literalAttributes
            );
            serializer.endElement(INTERNAL_XDM_URI, "literal", INTERNAL_XDM_PREFIX + ":literal");
        }
        serializer.endElement("", nodeName, nodeName);
    }
}
