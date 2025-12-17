package com.moa.service.template.impl;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import com.moa.service.template.TemplateRenderer;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ClasspathTemplateRenderer implements TemplateRenderer {

	private final ResourceLoader resourceLoader;

	@Override
	public String render(String classpathTemplatePath, Map<String, String> variables) {
		Resource resource = resourceLoader.getResource("classpath:" + classpathTemplatePath);

		try (InputStream is = resource.getInputStream()) {
			String html = new String(is.readAllBytes(), StandardCharsets.UTF_8);

			if (variables == null || variables.isEmpty()) {
				return html;
			}

			String rendered = html;
			for (Map.Entry<String, String> e : variables.entrySet()) {
				String key = e.getKey();
				String value = e.getValue() == null ? "" : escapeHtml(e.getValue());
				rendered = rendered.replace("${" + key + "}", value);
			}

			return rendered;
		} catch (Exception e) {
			throw new IllegalStateException("이메일 템플릿 로드 실패: " + classpathTemplatePath, e);
		}
	}

	private String escapeHtml(String v) {
		String s = v;
		s = s.replace("&", "&amp;");
		s = s.replace("<", "&lt;");
		s = s.replace(">", "&gt;");
		s = s.replace("\"", "&quot;");
		s = s.replace("'", "&#39;");
		return s;
	}
}
