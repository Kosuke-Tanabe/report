package actions;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import javax.servlet.ServletException;

import actions.views.EmployeeView;
import actions.views.ReportView;
import constants.AttributeConst;
import constants.ForwardConst;
import constants.JpaConst;
import constants.MessageConst;
import services.ReportService;

/**
 * 日報に関する処理を行うActionクラス
 *
 */
public class ReportAction extends ActionBase {

    private ReportService service;

    /**
     * メソッドを実行する
     */
    @Override
    public void process() throws ServletException, IOException {

        service = new ReportService();

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
        // 指定されたページ数の一覧画面に表示する日報データ取得
        int page = getPage();
        List<ReportView> reports = service.getAllPerPage(page);

        // 全日報データ件数取得
        long reportsCount = service.countAll();

        // 取得した日報データ
        putRequestScope(AttributeConst.REPORTS, reports);
        // 全ての日報データの件数
        putRequestScope(AttributeConst.REP_COUNT, reportsCount);
        // ページ数
        putRequestScope(AttributeConst.PAGE, page);
        // 1ページに表示するレコードの数
        putRequestScope(AttributeConst.MAX_ROW, JpaConst.ROW_PER_PAGE);

        // セッションにフラッシュメッセージが設定されている場合はリクエストスコープに移し替え、セッションからは削除する
        String flush = getSessionScope(AttributeConst.FLUSH);
        if (flush != null) {
            putRequestScope(AttributeConst.FLUSH, flush);
            removeSessionScope(AttributeConst.FLUSH);
        }

        // 一覧画面を表示
        forward(ForwardConst.FW_REP_INDEX);
    }

    /**
     * 新規登録画面を表示する
     * @throws ServletException
     * @throws IOException
     */
    public void entryNew() throws ServletException, IOException {
        //CSRF対策用トークン
        putRequestScope(AttributeConst.TOKEN, getTokenId());

        // 日報情報の空インスタンスに、日報の日付＝今日の日付を設定する
        ReportView rv = new ReportView();
        rv.setReportDate(LocalDate.now());
        // 日付のみ設定済みの日報インスタンス
        putRequestScope(AttributeConst.REP_DATE, rv);

        // 新規登録画面を表示
        forward(ForwardConst.FW_REP_NEW);
    }

    /**
     * 新規登録を行う
     * @throws ServletException
     * @throws IOException
     */
    public void create() throws ServletException, IOException {
        //CSRF対策 tokenのチェック
        if (checkToken()) {
            // 日報の日付が入力されていなければ今日の日付を設定
            LocalDate day = null;

            if (getRequestParam(AttributeConst.REP_DATE) == null
                    || getRequestParam(AttributeConst.REP_DATE).equals("")) {
                day = LocalDate.now();
            } else {
                day = LocalDate.parse(getRequestParam(AttributeConst.REP_DATE));
            }

            // セッションからログイン中の従業員情報取得
            EmployeeView ev = (EmployeeView) getSessionScope(AttributeConst.LOGIN_EMP);

            // パラメータを元に日報情報インスタンス作成
            ReportView rv = new ReportView(
                    null,
                    ev,
                    day,
                    getRequestParam(AttributeConst.REP_TITLE),
                    getRequestParam(AttributeConst.REP_CONTENT),
                    null,
                    null);

            // 日報情報登録
            List<String> errors = service.create(rv);

            if (errors.size() > 0) {
                // エラーの場合

                // CSRF対策用トークン
                putRequestScope(AttributeConst.TOKEN, getTokenId());
                // 入力された日報情報
                putRequestScope(AttributeConst.REPORT, rv);
                // エラーのリスト
                putRequestScope(AttributeConst.ERR, errors);

                // 新規登録画面表示
                forward(ForwardConst.FW_REP_NEW);
            } else {
                // エラーでない場合

                // セッションスコープに登録完了のフラッシュメッセージを設定
                putSessionScope(AttributeConst.FLUSH, MessageConst.I_REGISTERED.getMessage());

                // 一覧画面にリダイレクト
                redirect(ForwardConst.ACT_REP, ForwardConst.CMD_INDEX);
            }
        }
    }

    /**
     * 詳細画面を表示する
     * @throws ServletException
     * @throws IOException
     */
    public void show() throws ServletException, IOException {
        // idを条件に日報データを取得
        ReportView rv = service.findOne(toNumber(getRequestParam(AttributeConst.REP_ID)));

        if (rv == null) {
            // 該当の日報データが存在しない場合、エラー画面表示
            forward(ForwardConst.FW_ERR_UNKNOWN);
        } else {
            // エラーでない場合

            putRequestScope(AttributeConst.REPORT, rv);

            forward(ForwardConst.FW_REP_SHOW);

        }
    }

    /**
     * 編集画面を表示する
     * @throws ServletException
     * @throws IOException
     */
    public void edit() throws ServletException, IOException {
        // idを条件に日報データを取得
        ReportView rv = service.findOne(toNumber(getRequestParam(AttributeConst.REP_ID)));

        // セッションからログイン中の従業員情報取得
        EmployeeView ev = (EmployeeView) getSessionScope(AttributeConst.LOGIN_EMP);

        if (ev == null || ev.getId() != rv.getEmployee().getId()) {
            // 日報が存在しない、もしくは従業員IDが作成者のもので無い場合、エラー画面表示
            forward(ForwardConst.FW_ERR_UNKNOWN);
            return;
        } else {
            // CSRF対策用トークン
            putRequestScope(AttributeConst.TOKEN, getTokenId());
            // 日報データ
            putRequestScope(AttributeConst.REPORT, rv);

            // 編集画面を表示
            forward(ForwardConst.FW_REP_EDIT);
        }
    }

    /**
     * 更新を行う
     * @throws ServletException
     * @throws IOException
     */
    public void update() throws ServletException, IOException {
        // CSRF対策
        if (checkToken()) {
            // idを条件に日報データを取得
            ReportView rv = service.findOne(toNumber(getRequestParam(AttributeConst.REP_ID)));

            // 入力された内容を設定する
            rv.setReportDate(toLacalDate(getRequestParam(AttributeConst.REP_DATE)));
            rv.setTitle(getRequestParam(AttributeConst.REP_TITLE));
            rv.setContent(getRequestParam(AttributeConst.REP_CONTENT));

            // 日報データ更新
            List<String> errors = service.update(rv);

            if (errors.size() > 0) {
                // エラーの場合

                // CSRF対策用トークン
                putRequestScope(AttributeConst.TOKEN, getTokenId());
                // 入力された日報情報
                putRequestScope(AttributeConst.REPORT, rv);
                // エラーのリスト
                putRequestScope(AttributeConst.ERR, errors);
            } else {
                // エラーでない場合

                // セッションに更新完了のフラッシュメッセージを設定する
                putSessionScope(AttributeConst.FLUSH, MessageConst.I_UPDATED.getMessage());

                // 一覧画面にリダイレクト
                redirect(ForwardConst.ACT_REP, ForwardConst.CMD_INDEX);
            }
        }
    }
}
