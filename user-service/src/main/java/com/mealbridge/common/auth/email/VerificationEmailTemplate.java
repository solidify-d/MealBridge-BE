package com.mealbridge.common.auth.email;

final class VerificationEmailTemplate {

	private VerificationEmailTemplate() {
	}

	static String subject() {
		return "Your ConnectMeal verification code";
	}

	static String plainText(String code, int ttlMinutes) {
		return """
				ConnectMeal — Email verification

				Your verification code is: %s

				This code expires in %d minutes.

				If you did not request this, you can safely ignore this email.
				""".formatted(code, ttlMinutes);
	}

	static String html(String code, int ttlMinutes) {
		String spacedCode = String.join(" ", code.split(""));

		return """
				<!DOCTYPE html>
				<html lang="en">
				<head>
				  <meta charset="UTF-8" />
				  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
				  <title>ConnectMeal verification</title>
				</head>
				<body style="margin:0;padding:0;background-color:#f4f6f8;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif;">
				  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="background-color:#f4f6f8;padding:40px 16px;">
				    <tr>
				      <td align="center">
				        <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="max-width:520px;background-color:#ffffff;border-radius:16px;overflow:hidden;border:1px solid #e4e4e7;box-shadow:0 12px 40px rgba(1,42,74,0.08);">
				          <tr>
				            <td style="background:linear-gradient(135deg,#012a4a 0%%,#01497c 50%%,#2c7da0 100%%);padding:28px 32px;">
				              <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0">
				                <tr>
				                  <td>
				                    <p style="margin:0;font-size:13px;font-weight:600;letter-spacing:0.08em;text-transform:uppercase;color:rgba(255,255,255,0.75);">ConnectMeal</p>
				                    <p style="margin:8px 0 0;font-size:22px;font-weight:700;line-height:1.3;color:#ffffff;">Verify your email</p>
				                  </td>
				                  <td align="right" valign="top">
				                    <div style="width:40px;height:40px;border-radius:10px;background:rgba(255,255,255,0.15);text-align:center;line-height:40px;font-size:18px;font-weight:700;color:#ffffff;">CM</div>
				                  </td>
				                </tr>
				              </table>
				            </td>
				          </tr>
				          <tr>
				            <td style="padding:32px;">
				              <p style="margin:0 0 16px;font-size:15px;line-height:1.6;color:#52525b;">
				                Enter this code on the registration page to confirm your email address.
				              </p>
				              <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0">
				                <tr>
				                  <td align="center" style="background-color:#f8fafc;border:1px solid #e4e4e7;border-radius:12px;padding:24px 16px;">
				                    <p style="margin:0 0 8px;font-size:12px;font-weight:600;letter-spacing:0.06em;text-transform:uppercase;color:#71717a;">Your code</p>
				                    <p style="margin:0;font-size:36px;font-weight:700;letter-spacing:0.35em;color:#012a4a;font-family:ui-monospace,'SF Mono',Consolas,monospace;">%s</p>
				                  </td>
				                </tr>
				              </table>
				              <p style="margin:20px 0 0;font-size:13px;line-height:1.5;color:#71717a;text-align:center;">
				                Expires in <strong style="color:#3f3f46;">%d minutes</strong>
				              </p>
				              <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="margin-top:28px;">
				                <tr>
				                  <td style="background-color:#f4f6f8;border-radius:10px;padding:14px 16px;">
				                    <p style="margin:0;font-size:12px;line-height:1.5;color:#71717a;">
				                      If you did not request this code, you can ignore this email. Someone may have entered your address by mistake.
				                    </p>
				                  </td>
				                </tr>
				              </table>
				            </td>
				          </tr>
				          <tr>
				            <td style="padding:0 32px 28px;">
				              <p style="margin:0;font-size:11px;line-height:1.5;color:#a1a1aa;text-align:center;">
				                ConnectMeal · Surplus meal data platform<br />
				                This is an automated message — please do not reply.
				              </p>
				            </td>
				          </tr>
				        </table>
				      </td>
				    </tr>
				  </table>
				</body>
				</html>
				""".formatted(spacedCode, ttlMinutes);
	}
}
