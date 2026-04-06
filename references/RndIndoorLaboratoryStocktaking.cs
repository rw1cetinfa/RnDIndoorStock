using System;

namespace RnDLaboratoryStock.Core.Models;

public partial class RndIndoorLaboratoryStocktaking
{
    public int Id { get; set; }

    public int SessionId { get; set; }

    public string Cabinet { get; set; } = null!;

    public int Shelf { get; set; }

    public int EpcCount { get; set; }

    public DateTime? InsertedAt { get; set; }
}
