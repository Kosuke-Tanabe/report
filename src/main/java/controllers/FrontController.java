package controllers;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import actions.ActionBase;
import actions.UnknownAction;
import constants.ForwardConst;

/**
 * フロントコントローラー
 */

@WebServlet("/")
public class FrontController extends HttpServlet {
    private static final long serialVersionUID = 1L;

    public FrontController() {
        super();
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // パラメータに該当するActionクラスのインスタンス
        ActionBase action = getAction(request, response);

        // サーブレットコンテキスト、リクエスト、レスポンスをActionインスタンスのフィールドに設定
        action.init(getServletContext(), request, response);

        // Actionクラスの処理を呼び出し
        action.process();

    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }

    /**
     * リクエストパラメータの値から該当するActionクラスのインスタンスを作成し、返却する
     * (例:パラメータが action=Employee の場合、actions.EmployeeActionオブジェクト)
     * @param request リクエスト
     * @param response レスポンス
     * @return
     */
    @SuppressWarnings({ "rawtypes", "unchecked"}) // コンパイラ警告を抑止
    private ActionBase getAction(HttpServletRequest request, HttpServletResponse response) {
        Class type = null;
        ActionBase action = null;

        try {
            // リクエストからパラメータ"action"の値を取得（例："Employee"、"Report"）
            String actionString = request.getParameter(ForwardConst.ACT.getValue());

            // 該当するACTIONオブジェクトを作成(例：リクエストからパラメータ action=Employeeの場合、action.Employeeオブジェクト)
            type = Class.forName(String.format("actions.%sAction", actionString));

            // ActionBaseオブジェクトにキャスト
            action = (ActionBase)(type.asSubclass(ActionBase.class).getDeclaredConstructor().newInstance());

        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | SecurityException
                | IllegalArgumentException | InvocationTargetException| NoSuchMethodException e) {
            // リクエストからパラメータに設定されているactionの値が不正の場合
            // エラーしょりを行うActionオブジェクトを生成
            action = new UnknownAction();
        }

        return action;
    }

}
