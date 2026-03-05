package com.dtt.organization.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
@Controller
public class CustomErrorPageController implements ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        String message = "We encountered an unexpected issue.";

        if (status != null) {
            int statusCode = Integer.parseInt(status.toString());
            model.addAttribute("status", statusCode);

            if (statusCode == 404) {
                message = "The page you are looking for doesn't exist or has been moved.";
            } else if (statusCode == 403) {
                message = "You do not have the necessary permissions to view this page.";
            } else if (statusCode == 401) {
                message = "Your session has expired. Please log in again.";
            } else if (statusCode == 500) {
                message = "Our system is having trouble. Please try again later.";
            }
        } else {
            model.addAttribute("status", "Error");
        }

        model.addAttribute("errorMessage", message);
        return "error-page";
    }
}