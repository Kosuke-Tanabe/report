package actions;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;

import actions.views.EmployeeView;
import constants.AttributeConst;
import constants.ForwardConst;
import constants.JpaConst;
import constants.MessageConst;
import constants.PropertyConst;
import services.EmployeeService;

/**
 * 従業員に関わる処理を行うActionクラス
 *
 */
public class EmployeeAction extends ActionBase {

    private EmployeeService service;

    /**
     * メソッドを実行する
     */
    @Override
    public void process() throws ServletException, IOException {
        service = new EmployeeService();

        // メソッド実行
        invoke();

        service.close();
    }

    /**
     * 一覧画面を表示する
     * @throws ServletException
     * @throws IOException
     */
    public void index() throws ServletException, IOException {
        // 指定されたページ数の一覧画面にひょうじするデータを取得
        int page = getPage();
        List<EmployeeView> employees = service.getPerPage(page);

        // 全ての従業員データの件数を取得
        long employeeCount = service.countAll();

        //取得した従業員データ
        putRequestScope(AttributeConst.EMPLOYEES, employees);
        //全ての従業員データの件数
        putRequestScope(AttributeConst.EMP_COUNT, employeeCount);
        putRequestScope(AttributeConst.PAGE, page); //ページ数
        //1ページに表示するレコードの数
        putRequestScope(AttributeConst.MAX_ROW, JpaConst.ROW_PER_PAGE);

        // セッションにフラッシュメッセージが設定されている場合はリクエストスコープに移し替え、セッションからは削除する
        String flush = getSessionScope(AttributeConst.FLUSH);
        if (flush != null) {
            putRequestScope(AttributeConst.FLUSH, flush);
            removeSessionScope(AttributeConst.FLUSH);
        }

        // 一覧画面を表示
        forward(ForwardConst.FW_EMP_INDEX);
    }

    /**
     * 新規登録画面を表示する
     * @throws ServletException
     * @throws IOException
     */
    public void entryNew() throws ServletException, IOException {

        //CSRF対策用トークン
        putRequestScope(AttributeConst.TOKEN, getTokenId());
        // 空の従業員インスタンス
        putRequestScope(AttributeConst.EMPLOYEE, new EmployeeView());

        // 新規登録画面を表示
        forward(ForwardConst.FW_EMP_NEW);

    }

    /**
     * 新規登録を行う
     * @throws ServletException
     * @throws IOException
     */
    public void create() throws ServletException, IOException {
        // CSRF対策 tokenのチェック
        if (checkToken()) {
            // パラメータの値を元に従業員情報のインスタンスを作成する
            EmployeeView ev = new EmployeeView(
                    null,
                    getRequestParam(AttributeConst.EMP_CODE),
                    getRequestParam(AttributeConst.EMP_NAME),
                    getRequestParam(AttributeConst.EMP_PASS),
                    toNumber(getRequestParam(AttributeConst.EMP_ADMIN_FLG)),
                    null,
                    null,
                    AttributeConst.DEL_FLAG_FALSE.getIntegerValue());

            // アプリケーションスコープからpepper文字列を取得
            String pepper = getContextScope(PropertyConst.PEPPER);

            List<String> errors = service.create(ev, pepper);

            if (errors.size() > 0) {
                // エラーが存在する場合
                putRequestScope(AttributeConst.TOKEN, getTokenId());
                putRequestScope(AttributeConst.EMPLOYEE, ev);
                putRequestScope(AttributeConst.ERR, errors);

                // 新規画面表示
                forward(ForwardConst.FW_EMP_NEW);
            } else {
                // エラーが存在しない場合
                // セッションに登録完了のフラッシュメッセージを設定
                putSessionScope(AttributeConst.FLUSH, MessageConst.I_REGISTERED.getMessage());

                // 一覧画面にリダイレクト
                redirect(ForwardConst.ACT_EMP, ForwardConst.CMD_INDEX);
            }
        }
    }

    /**
     * 詳細画面を表示する
     * @throws ServletException
     * @throws IOException
     */
    public void show() throws ServletException, IOException {
        // idを条件に従業員データを取得する
        EmployeeView ev = service.findOne(toNumber(getRequestParam(AttributeConst.EMP_ID)));

        if (ev == null || ev.getDeleteFlag() == AttributeConst.DEL_FLAG_TRUE.getIntegerValue()) {
            // データが取得出来ない、もしくは論理削除されている場合、エラー画面表示
            forward(ForwardConst.FW_ERR_UNKNOWN);
            return;
        }

        putRequestScope(AttributeConst.EMPLOYEE, ev);

        // 詳細画面表示
        forward(ForwardConst.FW_EMP_SHOW);
    }

    /**
     * 編集画面を表示する
     * @throws ServletException
     * @throws IOException
     */
    public void edit() throws ServletException, IOException {
        // idを条件に従業員データを取得
        EmployeeView ev = service.findOne(toNumber(getRequestParam(AttributeConst.EMP_ID)));

        if (ev == null || ev.getDeleteFlag() == AttributeConst.DEL_FLAG_TRUE.getIntegerValue()) {
            // データが取得出来なかった、もしくは論理削除済みの場合エラー画面表示
            forward(ForwardConst.FW_ERR_UNKNOWN);
            return;
        }

        // CSRF対策 tokenのチェック
        putRequestScope(AttributeConst.TOKEN, getTokenId());
        // 取得した従業員番号
        putRequestScope(AttributeConst.EMPLOYEE, ev);

        // 変種画面を表示する
        forward(ForwardConst.FW_EMP_EDIT);
    }
}
