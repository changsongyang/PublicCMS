package com.publiccms.common.document;

import org.jsoup.helper.Validate;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Comment;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Safelist;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;

public class CustomCleaner extends Cleaner {

    private final Safelist safelist;

    public CustomCleaner(Safelist safelist) {
        super(safelist);
        this.safelist = safelist;
    }

    @Override
    public Document clean(Document dirtyDocument) {
        Validate.notNull(dirtyDocument);
        Document clean = Document.createShell(dirtyDocument.baseUri());
        copySafeNodes(dirtyDocument.body(), clean.body());
        clean.outputSettings(dirtyDocument.outputSettings().clone());
        return clean;
    }

    private int copySafeNodes(Element source, Element dest) {
        CleaningVisitor cleaningVisitor = new CleaningVisitor(source, dest);
        NodeTraversor.traverse(cleaningVisitor, source);
        return cleaningVisitor.numDiscarded;
    }

    private final class CleaningVisitor implements NodeVisitor {
        private int numDiscarded = 0;
        private final Element root;
        private Element destination; // current element to append nodes to

        private CleaningVisitor(Element root, Element destination) {
            this.root = root;
            this.destination = destination;
        }

        @Override
        public void head(Node source, int depth) {
            if (source instanceof Element) {
                Element sourceEl = (Element) source;
                if (safelist.isSafeTag(sourceEl.normalName())) { // safe, clone
                                                                 // and copy
                                                                 // safe attrs
                    ElementMeta meta = createSafeElement(sourceEl);
                    Element destChild = meta.el;
                    destination.appendChild(destChild);

                    numDiscarded += meta.numAttribsDiscarded;
                    destination = destChild;
                } else if (source != root) { // not a safe tag, so don't add.
                                             // don't count root against
                                             // discarded.
                    numDiscarded++;
                }
            } else if (source instanceof TextNode) {
                TextNode sourceText = (TextNode) source;
                TextNode destText = new TextNode(sourceText.getWholeText());
                destination.appendChild(destText);
            } else if (source instanceof Comment) {
                Comment sourceComment = (Comment) source;
                Comment destComment = new Comment(sourceComment.getData());
                destination.appendChild(destComment);
            } else if (source instanceof DataNode && safelist.isSafeTag(source.parent().normalName())) {
                DataNode sourceData = (DataNode) source;
                DataNode destData = new DataNode(sourceData.getWholeData());
                destination.appendChild(destData);
            } else { // else, we don't care about comments, xml proc
                     // instructions, etc
                numDiscarded++;
            }
        }

        @Override
        public void tail(Node source, int depth) {
            if (source instanceof Element && safelist.isSafeTag(source.normalName())) {
                destination = destination.parent(); // would have descended, so
                                                    // pop destination stack
            }
        }

        private ElementMeta createSafeElement(Element sourceEl) {
            Element dest = sourceEl.shallowClone(); // reuses tag, clones attributes
                                                    // and preserves any user data
            String sourceTag = sourceEl.tagName();
            Attributes destAttrs = dest.attributes();
            dest.clearAttributes(); // clear all non-internal attributes, ready for
                                    // safe copy

            int numAttribsDiscarded = 0;
            Attributes sourceAttrs = sourceEl.attributes();
            for (Attribute sourceAttr : sourceAttrs) {
                if (safelist.isSafeAttribute(sourceTag, sourceEl, sourceAttr))
                    destAttrs.put(sourceAttr);
                else
                    numAttribsDiscarded++;
            }
            Attributes enforcedAttrs = safelist.getEnforcedAttributes(sourceTag);
            destAttrs.addAll(enforcedAttrs);
            dest.attributes().addAll(destAttrs); // re-attach, if removed in clear
            return new ElementMeta(dest, numAttribsDiscarded);
        }
    }

    private static class ElementMeta {
        Element el;
        int numAttribsDiscarded;

        ElementMeta(Element el, int numAttribsDiscarded) {
            this.el = el;
            this.numAttribsDiscarded = numAttribsDiscarded;
        }
    }

}
