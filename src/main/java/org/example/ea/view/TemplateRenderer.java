package org.example.ea.view;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import freemarker.cache.ClassTemplateLoader;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;

public class TemplateRenderer {
    private final Configuration cfg;

    public TemplateRenderer() {
        cfg = new Configuration(Configuration.VERSION_2_3_34);
        cfg.setTemplateLoader(new ClassTemplateLoader(getClass(), "/templates"));
        cfg.setDefaultEncoding("UTF-8");
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        cfg.setFallbackOnNullLoopVariable(false);
    }

    public void render(String template, Map<String, Object> model, Writer writer) {
        try {
            Template tpl = cfg.getTemplate(template);
            tpl.process(model, writer);
        } catch (IOException | TemplateException e) {
            throw new IllegalStateException("Template rendering failed", e);
        }
    }
}

