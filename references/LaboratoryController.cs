using Azure;
using ClosedXML.Excel;
using DocumentFormat.OpenXml.Math;
using DocumentFormat.OpenXml.Office2010.Drawing;
using DocumentFormat.OpenXml.Spreadsheet;
using Microsoft.AspNetCore.Hosting;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Configuration;
using RnDLaboratoryStock.Core.Models;
using RnDLaboratoryStock.Data.Contexts;
using System.Data;
using System.Diagnostics;
using System.Diagnostics.Eventing.Reader;
using System.Globalization;
using System.Net;
using System.Net.Mail;
using System.Text;

namespace RnDLaboratoryStock.Controllers
{
    [Route("api/[controller]/[Action]")]
    [ApiController]
    public class LaboratoryController : CustomBaseController
    {
        private PcsContext _pcsContext;
        private TplContext _tplContext;
        private readonly IWebHostEnvironment _env;
        private readonly IConfiguration _configuration;
        public LaboratoryController(PcsContext pcsContext, TplContext tplContext, IWebHostEnvironment env, IConfiguration configuration)
        {
            _pcsContext = pcsContext;
            _tplContext = tplContext;
            _env = env;
            _configuration = configuration;
        }

        [HttpGet]
        public async Task<IActionResult> UserCheck(string workerCode)
        {
            ResponseModel<MdWorker> response = new ResponseModel<MdWorker>();

            try
            {
                var user = await _pcsContext.MdWorkers.Where(_ => _.WmCode == workerCode && _.WmSuspensionDate == null).FirstOrDefaultAsync();

                if (user == null)
                {
                    response.ResponseCode = 404;
                    response.Error = new ErrorModel { ErrorCode = 404, ErrorDesc = "Kullanıcı bulunamadı." };
                }
                else
                {
                    List<MdWorker> list = new List<MdWorker> { user };
                    response.Data = list;
                    response.ResponseCode = 200;
                }

            }
            catch (Exception ex)
            {
                response.ResponseCode = 500;
                response.Error = new ErrorModel { ErrorCode = 500, ErrorDesc = ex.Message };
                if (ex.InnerException != null)
                {
                    response.Error.InnerErrorDesc = ex.InnerException.Message;
                }
            }

            return ActionResultInstance(response);
        }

        [HttpPost]
        public async Task<IActionResult> StartSession(SessionModel model)
        {
            ResponseModel<RndLaboratorySession> response = new ResponseModel<RndLaboratorySession>();

            try
            {
                var session = new RndLaboratorySession { WmCode = model.WmCode, IsActive = true, LatestShelfNumber = 0, LatestCabinetNumber = model.CabinetNumber };

                await _tplContext.RndLaboratorySessions.AddAsync(session);
                await _tplContext.SaveChangesAsync();

                List<RndLaboratorySession> sessions = new List<RndLaboratorySession> { session };
                response.Data = sessions;
                response.ResponseCode = 200;

            }
            catch (Exception ex)
            {
                response.ResponseCode = 500;
                response.Error = new ErrorModel { ErrorCode = 500, ErrorDesc = ex.Message };
                if (ex.InnerException != null)
                {
                    response.Error.InnerErrorDesc = ex.InnerException.Message;
                }
            }

            return ActionResultInstance(response);
        }

        [HttpGet]
        public async Task<IActionResult> CheckSession(string wmCode)
        {
            ResponseModel<RndLaboratorySession> response = new ResponseModel<RndLaboratorySession>();

            try
            {

                var session = await _tplContext.RndLaboratorySessions.Where(_ => _.WmCode.Equals(wmCode) && _.IsActive == true).FirstOrDefaultAsync();

                if (session != null)
                {
                    List<RndLaboratorySession> sessions = new List<RndLaboratorySession> { session };
                    response.Data = sessions;
                    response.ResponseCode = 200;
                }
                else
                {
                    response.ResponseCode = 404;
                }

            }
            catch (Exception ex)
            {
                response.ResponseCode = 500;
                response.Error = new ErrorModel { ErrorCode = 500, ErrorDesc = ex.Message };
                if (ex.InnerException != null)
                {
                    response.Error.InnerErrorDesc = ex.InnerException.Message;
                }
            }

            return ActionResultInstance(response);
        }

        [HttpPut]
        public async Task<IActionResult> IncreaseSession(IncreaseShelfModel model)
        {
            ResponseModel<BlankModel> response = new ResponseModel<BlankModel>();

            try
            {
                if (model.Epclist.Count() == 0)
                {
                    response.ResponseCode = 200;
                    return ActionResultInstance(response);
                }

                var session = await _tplContext.RndLaboratorySessions.FindAsync(model.SessionId);

                if (session != null)
                {
                    var shelfs = await _tplContext.RndLaboratoryStocktakings.Where(_ => _.SessionId == session.Id).Select(_ => _.Id).ToListAsync();

                    List<string> clearedEpcs = new List<string>();
                    if (shelfs.Count() > 0)
                    {
                        List<string> relatedEpcs = new List<string>();

                        foreach (var item in shelfs)
                        {
                            var epcs = await _tplContext.RndLaboratoryStocktakingDetails.Where(_ => _.ParentId == item).Select(_ => _.Epc).ToListAsync();
                            relatedEpcs.AddRange(epcs);
                        }

                        clearedEpcs = model.Epclist.Except(relatedEpcs).ToList();

                        if (clearedEpcs.Count() == 0)
                        {
                            response.ResponseCode = 406;
                            response.Error = new ErrorModel { ErrorCode = 406 };
                            return ActionResultInstance(response);
                        }
                    }
                    else
                    {
                        clearedEpcs = model.Epclist;
                    }

                    session.LatestShelfNumber = model.ShelfNumber;
                    _tplContext.RndLaboratorySessions.Update(session);


                    var shelfDetails = await _tplContext.RndLaboratoryStocktakings.AddAsync(new RndLaboratoryStocktaking
                    {
                        Cabinet = model.CabinetNumber,
                        EpcCount = clearedEpcs.Count(),
                        Shelf = model.ShelfNumber,
                        SessionId = model.SessionId
                    });
                    await _tplContext.SaveChangesAsync();

                    foreach (var epc in clearedEpcs)
                    {

                        await _tplContext.RndLaboratoryStocktakingDetails.AddAsync(new RndLaboratoryStocktakingDetail
                        {
                            ParentId = shelfDetails.Entity.Id,
                            Epc = epc
                        });

                    }

                    await _tplContext.SaveChangesAsync();

                    if ((model.Epclist.Count() - clearedEpcs.Count()) != 0)
                    {
                        response.ResponseDesc = "Diğer rafa geçildi." + (model.Epclist.Count() - clearedEpcs.Count()) + " adet daha önce kaydedilmiş malzeme hariç tutuldu.";
                    }
                    else
                    {
                        response.ResponseDesc = "Diğer rafa geçildi.";

                    }

                    response.ResponseCode = 200;
                }
                else
                {

                    response.ResponseCode = 404;
                    response.Error = new ErrorModel { ErrorCode = 404 };
                }

            }
            catch (Exception ex)
            {
                response.ResponseCode = 500;
                response.Error = new ErrorModel { ErrorCode = 500, ErrorDesc = ex.Message };
                if (ex.InnerException != null)
                {
                    response.Error.InnerErrorDesc = ex.InnerException.Message;
                }
            }

            return ActionResultInstance(response);
        }

        [HttpPut]
        public async Task<IActionResult> UpdateSession(IncreaseShelfModel model)
        {
            ResponseModel<BlankModel> response = new ResponseModel<BlankModel>();

            try
            {


                var session = await _tplContext.RndLaboratorySessions.FindAsync(model.SessionId);

                if (session != null)
                {
                    session.LatestCabinetNumber = model.CabinetNumber;
                    session.LatestShelfNumber = model.ShelfNumber;

                    _tplContext.RndLaboratorySessions.Update(session);

                    await _tplContext.SaveChangesAsync();

                    response.ResponseCode = 200;
                }
                else
                {
                    response.ResponseCode = 404;
                }

            }
            catch (Exception ex)
            {
                response.ResponseCode = 500;
                response.Error = new ErrorModel { ErrorCode = 500, ErrorDesc = ex.Message };
                if (ex.InnerException != null)
                {
                    response.Error.InnerErrorDesc = ex.InnerException.Message;
                }
            }

            return ActionResultInstance(response);
        }

        [HttpGet]
        public async Task<IActionResult> EndSession(int sessionId, bool flag)
        {
            ResponseModel<BlankModel> response = new ResponseModel<BlankModel>();

            try
            {

                var session = await _tplContext.RndLaboratorySessions.FindAsync(sessionId);

                if (session != null && session.IsActive)
                {
                    session.IsActive = false;
                    session.ClosedAt = GetCurrentDatabaseDateAsync();
                    _tplContext.RndLaboratorySessions.Update(session);
                    await _tplContext.SaveChangesAsync();

                    if (flag)
                    {
                        var mailResponse = await SendMail(sessionId);


                        if (mailResponse)
                        {
                            response.ResponseCode = 200;
                        }
                        else
                        {
                            response.ResponseCode = 202;
                        }
                    }
                    else
                    {
                        response.ResponseCode = 200;
                    }

                }
                else
                {
                    response.ResponseCode = 404;
                    response.Error = new ErrorModel { ErrorCode = 404, ErrorDesc = "Aktif tarama mevcut değil." };
                }

            }
            catch (Exception ex)
            {
                response.ResponseCode = 500;
                response.Error = new ErrorModel { ErrorCode = 500, ErrorDesc = ex.Message };
                if (ex.InnerException != null)
                {
                    response.Error.InnerErrorDesc = ex.InnerException.Message;
                }
            }

            return ActionResultInstance(response);
        }

        [HttpGet]
        public async Task<IActionResult> CheckRFIDAvailability(string epc)
        {
            ResponseModel<BlankModel> response = new ResponseModel<BlankModel>();

            try
            {

                var session = await _tplContext.RndLaboratoryMaterials.Where(_ => _.Epc.Equals(epc)).FirstOrDefaultAsync();

                if (session != null)
                {
                    response.ResponseCode = 200;
                }
                else
                {
                    response.ResponseCode = 404;
                    response.Error = new ErrorModel { ErrorCode = 404, ErrorDesc = "Rfid bulunamadı." };
                }

            }
            catch (Exception ex)
            {
                response.ResponseCode = 500;
                response.Error = new ErrorModel { ErrorCode = 500, ErrorDesc = ex.Message };
                if (ex.InnerException != null)
                {
                    response.Error.InnerErrorDesc = ex.InnerException.Message;
                }
            }

            return ActionResultInstance(response);
        }

        [HttpGet]
        public async Task<IActionResult> GetMaterialDetailsByRFID(string epc)
        {
            ResponseModel<RndLaboratoryMaterial> response = new ResponseModel<RndLaboratoryMaterial>();

            try
            {

                var material = await _tplContext.RndLaboratoryMasters.Where(_ => _.Epc.Equals(epc)).FirstOrDefaultAsync();

                if (material != null)
                {
                    var newMaterial = new RndLaboratoryMaterial {
                        Brand = material.Brand,
                        BrandCode = material.Code,
                        Amount = Convert.ToDouble(material.PackageQuantity),
                        Unit = material.Unit,
                        ChemicalName = material.ChemicalName,
                        CompanyName = material.SupplierName,
                        ItemNumber = Convert.ToInt32(material.ProductNumber),
                        Remain = Convert.ToInt32(material.Quantity)


                    };
                    List<RndLaboratoryMaterial> list = new List<RndLaboratoryMaterial>();
                    list.Add(newMaterial);
                    response.Data = list;
                    response.ResponseCode = 200;
                }
                else
                {
                    response.ResponseCode = 404;
                    response.Error = new ErrorModel { ErrorCode = 404, ErrorDesc = "Materyal bulunamadı." };
                }

            }
            catch (Exception ex)
            {
                response.ResponseCode = 500;
                response.Error = new ErrorModel { ErrorCode = 500, ErrorDesc = ex.Message };
                if (ex.InnerException != null)
                {
                    response.Error.InnerErrorDesc = ex.InnerException.Message;
                }
            }

            return ActionResultInstance(response);
        }

        [HttpGet]
        public async Task<IActionResult> GetRSSI()
        {
            ResponseModel<RndLaboratoryRssiFilter> response = new ResponseModel<RndLaboratoryRssiFilter>();

            try
            {

                var rssi = await _tplContext.RndLaboratoryRssiFilters.FirstOrDefaultAsync();

                if (rssi != null)
                {
                    List<RndLaboratoryRssiFilter> rssiList = new List<RndLaboratoryRssiFilter>();
                    rssiList.Add(rssi);

                    response.Data = rssiList;
                    response.ResponseCode = 200;
                }
                else
                {
                    response.ResponseCode = 404;
                    response.Error = new ErrorModel { ErrorCode = 404, ErrorDesc = "RSSI değeri bulunamadı." };

                }

            }
            catch (Exception ex)
            {
                response.ResponseCode = 500;
                response.Error = new ErrorModel { ErrorCode = 500, ErrorDesc = ex.Message };
                if (ex.InnerException != null)
                {
                    response.Error.InnerErrorDesc = ex.InnerException.Message;
                }
            }

            return ActionResultInstance(response);
        }

        [HttpPost]
        public async Task<IActionResult> InsertEpcDetails(RndLaboratoryMaster epc)
        {
            ResponseModel<BlankModel> response = new ResponseModel<BlankModel>();

            try
            {
                epc.SupplierName = "";
                epc.QuantityNumber = "";
                epc.Unit = "";
                epc.ChemicalName = "";
                epc.Limit = 0;
                epc.ExpireLimit = 9999;
                epc.InsertedAt = GetCurrentDatabaseDateAsync();


                var rssiDb = await _tplContext.RndLaboratoryMasters.AddAsync(epc);
                await _tplContext.SaveChangesAsync();


                response.ResponseCode = 200;

            }
            catch (Exception ex)
            {
                response.ResponseCode = 500;
                response.Error = new ErrorModel { ErrorCode = 500, ErrorDesc = ex.Message };
                if (ex.InnerException != null)
                {
                    response.Error.InnerErrorDesc = ex.InnerException.Message;
                }
            }

            return ActionResultInstance(response);
        }


        [HttpPut]
        public async Task<IActionResult> UpdateRSSI(int rssi)
        {
            ResponseModel<BlankModel> response = new ResponseModel<BlankModel>();

            try
            {

                var rssiDb = await _tplContext.RndLaboratoryRssiFilters.FirstOrDefaultAsync();

                if (rssiDb != null)
                {
                    rssiDb.Rssi = rssi;
                    _tplContext.RndLaboratoryRssiFilters.Update(rssiDb);
                    await _tplContext.SaveChangesAsync();

                    response.ResponseCode = 200;
                }
                else
                {
                    var newRssi = new RndLaboratoryRssiFilter { Rssi = rssi };
                    await _tplContext.RndLaboratoryRssiFilters.AddAsync(newRssi);
                    await _tplContext.SaveChangesAsync();
                    response.ResponseCode = 200;
                }

            }
            catch (Exception ex)
            {
                response.ResponseCode = 500;
                response.Error = new ErrorModel { ErrorCode = 500, ErrorDesc = ex.Message };
                if (ex.InnerException != null)
                {
                    response.Error.InnerErrorDesc = ex.InnerException.Message;
                }
            }

            return ActionResultInstance(response);
        }

        [HttpPut("{wm_code}")]
        public async Task<IActionResult> DeleteByEpcs([FromBody] List<string> epcs, string wm_code)
        {
            ResponseModel<BlankModel> response = new ResponseModel<BlankModel>();

            try
            {
                var formattedEpcs = string.Join(",", epcs.Select(e => $"'{e}'"));
                var epcDelete = await _tplContext.RndLaboratoryMasters
                    .FromSqlRaw($"SELECT * FROM RND_LABORATORY_MASTER WHERE EPC IN ({formattedEpcs})")
                    .ToListAsync();

                if (epcDelete.Count() > 0)
                {
                    _tplContext.RndLaboratoryMasters.RemoveRange(epcDelete);

                    var archiveItems = epcDelete.Select(item => new RndLaboratoryMasterDeleted
                    {
                        OldId = item.Id,
                        Epc = item.Epc,
                        ProductNumber = item.ProductNumber,
                        ExpiredDate = item.ExpiredDate,
                        CabinetNumber = item.CabinetNumber,
                        Shelf = item.Shelf,
                        Quantity = item.Quantity,
                        Brand = item.Brand,
                        Code = item.Code,
                        PackageQuantity = item.PackageQuantity,
                        Unit = item.Unit,
                        ChemicalName = item.ChemicalName,
                        SupplierName = item.SupplierName,
                        QuantityNumber = item.QuantityNumber,
                        Limit = item.Limit,
                        ExpireLimit = item.ExpireLimit,
                        InsertedAt = item.InsertedAt,
                        DeletedAt = GetCurrentDatabaseDateAsync(),
                        WmCode = wm_code
                    }).ToList();

                    await _tplContext.RndLaboratoryMasterDeleted.AddRangeAsync(archiveItems);
                    await _tplContext.SaveChangesAsync();

                    response.ResponseCode = 200;
                }
                else
                {
                    response.ResponseCode = 404;
                    response.Error = new ErrorModel { ErrorCode = 404, ErrorDesc = "RFID'ler bulunamadı." };
                }

            }
            catch (Exception ex)
            {
                response.ResponseCode = 500;
                response.Error = new ErrorModel { ErrorCode = 500, ErrorDesc = ex.Message };
                if (ex.InnerException != null)
                {
                    response.Error.InnerErrorDesc = ex.InnerException.Message;
                }
            }

            return ActionResultInstance(response);
        }

        //[HttpGet]
        //public async Task<IActionResult> Download(int sessionId)
        //{
        //    //ResponseModel<BlankModel> response = new ResponseModel<BlankModel>();

        //    var session = await _tplContext.RndLaboratorySessions.FindAsync(sessionId);

        //    if (session != null)
        //    {
        //        var folderPath = Path.Combine(_env.WebRootPath, "files");
        //        if (!Directory.Exists(folderPath))
        //        {
        //            Directory.CreateDirectory(folderPath);
        //        }

        //        var fileName = "StockTakingReport_" + DateTime.Now.ToString("yyyyMMddHHmmss") + ".xlsx";
        //        var filePath = Path.Combine(folderPath, fileName);

        //        using var workbook = new XLWorkbook();

        //        // Sheet1
        //        var ws1 = workbook.Worksheets.Add("Session_Details");

        //        string[] headers1 = { "NAME", "SURNAME", "WORKER CODE", "START DATE", "END DATE" };

        //        // Başlıklar
        //        for (int i = 0; i < headers1.Length; i++)
        //        {
        //            var cell = ws1.Cell(1, i + 1);
        //            cell.Value = headers1[i].ToUpper();
        //            cell.Style.Font.Bold = true;
        //        }

        //        var worker = await _pcsContext.MdWorkers.Where(_ => _.WmCode.Equals(session.WmCode)).FirstOrDefaultAsync();
        //        var name = "-";
        //        var surName = "-";

        //        if (worker != null)
        //        {
        //            name = worker.WmName.ToUpper();
        //            surName = worker.WmSurname.ToUpper();
        //        }

        //        // Satır verileri
        //        ws1.Cell(2, 1).Value = name;
        //        ws1.Cell(2, 2).Value = surName;
        //        ws1.Cell(2, 3).Value = session.WmCode;
        //        ws1.Cell(2, 4).Value = session.InsertedAt;
        //        ws1.Cell(2, 5).Value = session.ClosedAt;


        //        // Otomatik kolon genişliði
        //        ws1.Columns().AdjustToContents();

        //        // Tabloyu çerçeveli yap
        //        var range1 = ws1.Range(1, 1, 2, headers1.Length); // 1. satır başlık, 3. satır son veri
        //        range1.Style.Border.OutsideBorder = XLBorderStyleValues.Thin;
        //        range1.Style.Border.InsideBorder = XLBorderStyleValues.Thin;

        //        // Sheet2
        //        var ws2 = workbook.Worksheets.Add("Stocktaking");
        //        string[] headers2 = { "CABINET", "SHELF", "COUNT", "DATE" };
        //        for (int i = 0; i < headers2.Length; i++)
        //        {
        //            var cell = ws2.Cell(1, i + 1);
        //            cell.Value = headers2[i].ToUpper();
        //            cell.Style.Font.Bold = true;
        //        }

        //        var stoctaking = await _tplContext.RndLaboratoryStocktakings.Where(_ => _.SessionId == session.Id).OrderBy(_ => _.InsertedAt).ToListAsync();

        //        // Sheet3
        //        var ws3 = workbook.Worksheets.Add("Stocktaking_Details");
        //        string[] headers3 = { "CABINET", "SHELF", "EPC" };
        //        for (int i = 0; i < headers3.Length; i++)
        //        {
        //            var cell = ws3.Cell(1, i + 1);
        //            cell.Value = headers3[i].ToUpper();
        //            cell.Style.Font.Bold = true;
        //        }

        //        int j = 1;
        //        int k = 1;

        //        var range2 = ws2.Range(1, 1, j, headers2.Length);
        //        range2.Style.Border.OutsideBorder = XLBorderStyleValues.Thin;
        //        range2.Style.Border.InsideBorder = XLBorderStyleValues.Thin;

        //        foreach (var item in stoctaking)
        //        {
        //            j++;
        //            ws2.Cell(j, 1).Value = item.Cabinet;
        //            ws2.Cell(j, 2).Value = item.Shelf;
        //            ws2.Cell(j, 3).Value = item.EpcCount;
        //            ws2.Cell(j, 4).Value = item.InsertedAt;
        //            ws2.Columns().AdjustToContents();

        //            var stocktakingDetails = await _tplContext.RndLaboratoryStocktakingDetails.Where(_ => _.ParentId == item.Id).ToListAsync();
        //            foreach (var item2 in stocktakingDetails)
        //            {
        //                k++;

        //                ws3.Cell(k, 1).Value = item.Cabinet;
        //                ws3.Cell(k, 2).Value = item.Shelf;
        //                ws3.Cell(k, 3).Value = item2.Epc;
        //                ws3.Columns().AdjustToContents();
        //            }

        //        }


        //        var range3 = ws3.Range(1, 1, k, headers3.Length);
        //        range3.Style.Border.OutsideBorder = XLBorderStyleValues.Thin;
        //        range3.Style.Border.InsideBorder = XLBorderStyleValues.Thin;

        //        // MemoryStream’e yaz
        //        using var stream = new MemoryStream();
        //        workbook.SaveAs(stream);
        //        stream.Position = 0;

        //        workbook.SaveAs(filePath);

        //        return File(
        //            stream.ToArray(),
        //            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        //            fileName
        //        );

        //        //response.ResponseCode = 200;
        //    }
        //    else
        //    {
        //        //response.ResponseCode = 404;
        //        //response.Error = new ErrorModel { ErrorCode = 404, ErrorDesc = "Aktif tarama mevcut deðil." };
        //        return Ok();
        //    }

        //}

        [ApiExplorerSettings(IgnoreApi = true)]
        public DateTime GetCurrentDatabaseDateAsync()
        {
            using (var command = _tplContext.Database.GetDbConnection().CreateCommand())
            {
                command.CommandText = "SELECT GETDATE() AS CurrentDate";
                command.CommandType = CommandType.Text;

                _tplContext.Database.OpenConnection();

                using (var reader = command.ExecuteReader())
                {
                    if (reader.Read())
                    {
                        return reader.GetDateTime(0);
                    }
                }

                return DateTime.MinValue; // fallback
            }
        }

        //[ApiExplorerSettings(IgnoreApi = true)]
        [HttpGet]
        public async Task<bool> SendMail(int sessionId)
        {
            try
            {
                var session = await _tplContext.RndLaboratorySessions.FindAsync(sessionId);

                if (session != null)
                {
                    var folderPath = Path.Combine(_env.WebRootPath, "files");
                    if (!Directory.Exists(folderPath))
                    {
                        Directory.CreateDirectory(folderPath);
                    }

                    var fileName = "StockTakingReport_" + DateTime.Now.ToString("yyyyMMddHHmmss") + ".xlsx";
                    var filePath = Path.Combine(folderPath, fileName);

                    using var workbook = new XLWorkbook();

                    // Sheet1
                    var ws1 = workbook.Worksheets.Add("Session_Details");

                    string[] headers1 = { "NAME", "SURNAME", "WORKER CODE", "START DATE", "END DATE" };

                    // Başlıklar
                    for (int i = 0; i < headers1.Length; i++)
                    {
                        var cell = ws1.Cell(1, i + 1);
                        cell.Value = headers1[i].ToUpper();
                        cell.Style.Font.Bold = true;
                    }

                    var worker = await _pcsContext.MdWorkers.Where(_ => _.WmCode.Equals(session.WmCode)).FirstOrDefaultAsync();
                    var name = "-";
                    var surName = "-";

                    if (worker != null)
                    {
                        name = worker.WmName.ToUpper();
                        surName = worker.WmSurname.ToUpper();
                    }

                    // Satır verileri
                    ws1.Cell(2, 1).Value = name;
                    ws1.Cell(2, 2).Value = surName;
                    ws1.Cell(2, 3).Value = session.WmCode;
                    ws1.Cell(2, 4).Value = session.InsertedAt;
                    ws1.Cell(2, 5).Value = session.ClosedAt;


                    // Otomatik kolon genişliði
                    ws1.Columns().AdjustToContents();

                    // Tabloyu çerçeveli yap
                    var range1 = ws1.Range(1, 1, 2, headers1.Length); // 1. satır başlık, 3. satır son veri
                    range1.Style.Border.OutsideBorder = XLBorderStyleValues.Thin;
                    range1.Style.Border.InsideBorder = XLBorderStyleValues.Thin;

                    // Sheet2
                    var ws2 = workbook.Worksheets.Add("Stocktaking");
                    string[] headers2 = { "CABINET", "SHELF", "COUNT", "DATE" };
                    for (int i = 0; i < headers2.Length; i++)
                    {
                        var cell = ws2.Cell(1, i + 1);
                        cell.Value = headers2[i].ToUpper();
                        cell.Style.Font.Bold = true;
                    }

                    var stoctaking = await _tplContext.RndLaboratoryStocktakings.Where(_ => _.SessionId == session.Id).OrderBy(_ => _.InsertedAt).ToListAsync();

                    // Sheet3
                    var ws3 = workbook.Worksheets.Add("Stocktaking_Details");
                    string[] headers3 = { "CABINET", "SHELF", "EPC", "BRAND", "CHEMICAL NAME" };
                    for (int i = 0; i < headers3.Length; i++)
                    {
                        var cell = ws3.Cell(1, i + 1);
                        cell.Value = headers3[i].ToUpper();
                        cell.Style.Font.Bold = true;
                    }

                    int j = 1;
                    int k = 1;

                    var range2 = ws2.Range(1, 1, j, headers2.Length);
                    range2.Style.Border.OutsideBorder = XLBorderStyleValues.Thin;
                    range2.Style.Border.InsideBorder = XLBorderStyleValues.Thin;

                    List<RndLaboratoryMaster> masterList = new List<RndLaboratoryMaster>();

                    foreach (var item in stoctaking)
                    {
                        j++;
                        ws2.Cell(j, 1).Value = item.Cabinet;
                        ws2.Cell(j, 2).Value = item.Shelf;
                        ws2.Cell(j, 3).Value = item.EpcCount;
                        ws2.Cell(j, 4).Value = item.InsertedAt;
                        ws2.Columns().AdjustToContents();

                        var stocktakingDetails = await _tplContext.RndLaboratoryStocktakingDetails.Where(_ => _.ParentId == item.Id).ToListAsync();
                        foreach (var item2 in stocktakingDetails)
                        {

                            var labItem = await _tplContext.RndLaboratoryMasters.Where(_ => _.Epc.Equals(item2.Epc)).FirstOrDefaultAsync();
                            if (labItem != null)
                            {
                                masterList.Add(labItem);
                            }

                            k++;

                            ws3.Cell(k, 1).Value = item.Cabinet;
                            ws3.Cell(k, 2).Value = item.Shelf;
                            ws3.Cell(k, 3).Value = item2.Epc;
                            ws3.Cell(k, 4).Value = labItem != null ? labItem.Brand : "";
                            ws3.Cell(k, 5).Value = labItem != null ? labItem.ChemicalName : "";
                            ws3.Columns().AdjustToContents();

                        }

                    }


                    var range3 = ws3.Range(1, 1, k, headers3.Length);
                    range3.Style.Border.OutsideBorder = XLBorderStyleValues.Thin;
                    range3.Style.Border.InsideBorder = XLBorderStyleValues.Thin;




                    // Sheet4
                    var ws4 = workbook.Worksheets.Add("Stocktaking_Limits");
                    string[] headers4 = { "CHEMICAL NAME", "COUNT", "LIMIT" };
                    for (int i = 0; i < headers4.Length; i++)
                    {
                        var cell = ws4.Cell(1, i + 1);
                        cell.Value = headers4[i].ToUpper();
                        cell.Style.Font.Bold = true;
                    }


                    var materialLimits = masterList
                    .GroupBy(m => new { m.ChemicalName, m.Limit })
                    .Select(g => new
                    {
                        ChemicalName = g.Key.ChemicalName,
                        Count = g.Count(),
                        Limit = g.Key.Limit
                    })
                    .ToList();

                    List<RndLaboratoryMaster> expiredLimitList = new List<RndLaboratoryMaster>();

                    for (int i = 2; i < materialLimits.Count() + 2; i++)
                    {
                        ws4.Cell(i, 1).Value = materialLimits[i - 2].ChemicalName;
                        ws4.Cell(i, 2).Value = materialLimits[i - 2].Count;
                        ws4.Cell(i, 3).Value = materialLimits[i - 2].Limit;
                        ws4.Columns().AdjustToContents();

                        if ((materialLimits[i - 2].Count <= materialLimits[i - 2].Limit) && materialLimits[i - 2].Limit != 0)
                        {
                            // Tüm satırı seç (A ve B sütunları)
                            var rowRange = ws4.Range($"A{i}:C{i}");

                            // Yazı rengini beyaz yap
                            rowRange.Style.Font.FontColor = XLColor.White;

                            // Arka plan rengini örnek olarak kırmızı yap
                            rowRange.Style.Fill.BackgroundColor = XLColor.Red;
                            expiredLimitList.Add(new RndLaboratoryMaster
                            {
                                ChemicalName = materialLimits[i - 2].ChemicalName,
                                Limit = materialLimits[i - 2].Limit,
                                Shelf = materialLimits[i - 2].Count
                            }
                            );
                        }
                    }

                    var range4 = ws4.Range(1, 1, materialLimits.Count() + 1, headers4.Length);
                    range4.Style.Border.OutsideBorder = XLBorderStyleValues.Thin;
                    range4.Style.Border.InsideBorder = XLBorderStyleValues.Thin;


                    // Sheet5
                    var ws5 = workbook.Worksheets.Add("Stocktaking_Expired_Date");
                    string[] headers5 = { "CHEMICAL NAME", "BRAND", "CABINET", "SELF", "NUMBER", "EXPIRED_DATE" };
                    for (int i = 0; i < headers5.Length; i++)
                    {
                        var cell = ws5.Cell(1, i + 1);
                        cell.Value = headers5[i].ToUpper();
                        cell.Style.Font.Bold = true;
                    }

                    int m = 1;

                    List<RndLaboratoryMaster> expiredList = new List<RndLaboratoryMaster>();

                    foreach (var item in masterList)
                    {
                        m++;
                        ws5.Cell(m, 1).Value = item.ChemicalName;
                        ws5.Cell(m, 2).Value = item.Brand;
                        ws5.Cell(m, 3).Value = item.CabinetNumber;
                        ws5.Cell(m, 4).Value = item.Shelf;
                        ws5.Cell(m, 5).Value = item.Quantity;
                        ws5.Cell(m, 6).Value = item.ExpiredDate;
                        ws5.Columns().AdjustToContents();
                        if (item.ExpiredDate != null && DateTime.Now.AddDays((item.ExpireLimit == null ? 0 : (int)item.ExpireLimit)) > item.ExpiredDate)
                        {
                            // Tüm satırı seç (A ve B sütunları)
                            var rowRange = ws5.Range($"A{m}:F{m}");

                            // Yazı rengini beyaz yap
                            rowRange.Style.Font.FontColor = XLColor.White;

                            // Arka plan rengini örnek olarak kırmızı yap
                            rowRange.Style.Fill.BackgroundColor = XLColor.Red;

                            expiredList.Add(item);
                        }

                    }

                    var range5 = ws5.Range(1, 1, m, headers5.Length);
                    range5.Style.Border.OutsideBorder = XLBorderStyleValues.Thin;
                    range5.Style.Border.InsideBorder = XLBorderStyleValues.Thin;

                    // MemoryStream’e yaz
                    using var stream = new MemoryStream();
                    workbook.SaveAs(stream);
                    stream.Position = 0;





                    var pathToFile = _env.WebRootPath + Path.DirectorySeparatorChar.ToString() + "Mail" + Path.DirectorySeparatorChar.ToString() + "InfoMail.html";

                    string body = string.Empty;

                    using (StreamReader sourceReader = System.IO.File.OpenText(pathToFile))
                    {
                        body = sourceReader.ReadToEnd();
                    }
                    //body = body.Replace("{{UserName}}", user.FirstName + " " + user.LastName);


                    string cssFilePath = _env.WebRootPath + Path.DirectorySeparatorChar.ToString() + "css" + Path.DirectorySeparatorChar.ToString() + "style.css";
                    string cssContent = "";
                    using (StreamReader sourceReader = System.IO.File.OpenText(cssFilePath))
                    {
                        cssContent = sourceReader.ReadToEnd();
                    }
                    body = body.Replace("{{style}}", cssContent.ToString());


                    string imagePath = _env.WebRootPath + Path.DirectorySeparatorChar.ToString() + "images" + Path.DirectorySeparatorChar.ToString() + "Prometeon.png";
                    string base64Image = Convert.ToBase64String(System.IO.File.ReadAllBytes(imagePath));
                    string imageSrc = $"data:image/jpeg;base64,{base64Image}";

                    body = body.Replace("{{imageSrc}}", imageSrc);

                    int n = 1;
                    var tableRows = new StringBuilder();
                    foreach (var item in expiredList)
                    {
                        tableRows.AppendLine("<tr><th scope=\"row\">" + n + "</th>");
                        tableRows.AppendLine("<td>" + item.ChemicalName + "</td>");
                        tableRows.AppendLine("<td>" + item.Brand + "</td>");
                        tableRows.AppendLine("<td>" + item.CabinetNumber + "</td>");
                        tableRows.AppendLine("<td>" + item.Shelf + "</td>");
                        tableRows.AppendLine("<td>" + item.Quantity + "</td>");
                        tableRows.AppendLine("<td>" + (item.ExpiredDate == null ? "-" : ((DateTime)item.ExpiredDate).ToString("yyyy-MM-dd HH:mm:ss", CultureInfo.InvariantCulture)) + "</td>");

                    }
                    body = body.Replace("{{Rows}}", tableRows.ToString());

                    var limitRows = new StringBuilder();

                    int l = 1;
                    foreach (var item in expiredLimitList)
                    {
                        limitRows.AppendLine("<tr><th scope=\"row\">" + l + "</th>");
                        limitRows.AppendLine("<td>" + item.ChemicalName + "</td>");
                        limitRows.AppendLine("<td>" + item.Limit + "</td>");
                        limitRows.AppendLine("<td>" + item.Shelf + "</td>");
                    }

                    body = body.Replace("{{Rows2}}", limitRows.ToString());


                    var smtpClient = new SmtpClient
                    {
                        Host = _configuration.GetConnectionString("SMTP"),
                        // Port = port,
                        // EnableSsl = enable,
                        // Credentials = new NetworkCredential(username, password)
                    };
                    smtpClient.Credentials = CredentialCache.DefaultNetworkCredentials;

                    var mailMessage = new MailMessage { From = new MailAddress("noreply@prometeon.com", "Kimya Laboratuvarı Envanter Sayım Raporu") };

                    var mailList = await _tplContext.RndLaboratoryMailLists.ToListAsync();
                    foreach (var mail in mailList)
                    {
                        mailMessage.To.Add(mail.Email);
                    }

                    mailMessage.Subject = "Kimya Laboratuvarı Envanter Sayım Raporu";
                    mailMessage.Body = body;
                    mailMessage.IsBodyHtml = true;

                    var attachment = new Attachment(new MemoryStream(stream.ToArray()), fileName,
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                    mailMessage.Attachments.Add(attachment);

                    smtpClient.Send(mailMessage);

                    return true;
                }
                else
                {
                    return false;
                }


            }
            catch (Exception ex)
            {

                return false;
            }


        }
    }
}
